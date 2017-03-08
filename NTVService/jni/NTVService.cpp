
#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#include <pwd.h>
#include <syslog.h>
#include <pthread.h>
#include <stdlib.h>

#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <netdb.h>
#include <fcntl.h>
#include <net/if.h>
#include <netinet/tcp.h>
#include <arpa/inet.h> 
#include <memory.h>

#include <signal.h>
#include <event.h>
#include <event2/bufferevent.h>
#include <event2/buffer.h>
#include <event2/listener.h>
#include <event2/util.h>
#include <event2/event.h>
#include <pthread.h>
#include <event2/thread.h>


#include "x_thread.h"
#include "x_queue.h"
#include "x_time.h"
#include "x_lock.h"



#include "tsheader.h"

#include <android/log.h>
#include <jni.h>


#define tag "native-NTVService"
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, tag, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, tag, __VA_ARGS__))




#define TS_PORT 11234	//The port on which to listen for incoming data
#define RC_PORT 11235	//The port on which to listen for incoming data

#define SERVER_PORT 1234	
#define queueSize	1024*512



// ts_720_480;
// ts_1280_720;
// ts_1920_1080;
// default ts_720_480
unsigned char *tsheader = ts_720_480;


unsigned char tmpbuf[queueSize];

x_queue *g_data_queue;
x_lock g_queue_lock;
struct event_base *g_base;

int g_stream_socket;
char NTV_Key[256] = {0};


struct bufferevent *g_bev;
#define max_user  3



typedef struct ntvuser
{
	int uses;
	int login;
	bufferevent *bev;
} ntvuser_t;

ntvuser_t user_array[max_user];




ntvuser_t * UserConnect(bufferevent *bev)
{
	
	for(int x = 0; x < max_user; x++) {
		if (user_array[x].uses == 0) {
			user_array[x].uses = 1;
			user_array[x].login = 0;
			user_array[x].bev = bev;
			return &user_array[x];
		}
	}	
	return NULL;
	
}


void  UserDisconnect(ntvuser_t *pUser)
{
	
	for (int x = 0; x < max_user; x++) {
		if (&user_array[x] == pUser) {
			user_array[x].uses = 0;
			bufferevent_free(pUser->bev);
		}
	}	

	
}

/*---------------------------------------------------------------------------------------
*  
* 
---------------------------------------------------------------------------------------*/
class RCClient
{
	struct sockaddr_in si;
	int s;
	
public:	

	RCClient() {
		
		if ( (s=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0) {
			LOGI("RCClient socket() failed");
			return;
		}		
		
		int reuse	= 1;
		setsockopt(s, SOL_SOCKET, SO_REUSEADDR, (const char *)&reuse, sizeof(reuse));		
		
		//setup address structure
		memset((char *) &si, 0, sizeof(si));
		si.sin_family = AF_INET;
		si.sin_port = htons(RC_PORT);
		si.sin_addr.s_addr = inet_addr("127.0.0.1");
		
	}
	
	~RCClient() {
		if(s >= 0)
			close(s);
	}
	
	int send(char *data, int len)
	{
		if(s >= 0)
			return sendto(s, data, len , 0 , (struct sockaddr *) &si, sizeof(si));
		
		return -1;
	}
	
};

RCClient *g_pRC = NULL;

/*---------------------------------------------------------------------------------------
* 新客戶端連線觸發
* 這裡為新的客戶端配置一個 bufferevent 並設定 讀取 與 寫入完畢 與 離線的callback
---------------------------------------------------------------------------------------*/
int init_daemon(void)
{
    pid_t pid = fork();

    if( pid != 0 ) exit(0);//parent

    //first children
    if(setsid() == -1) {
		LOGI("setsid failed");
        exit(-1);
    }

    umask(0);

    pid = fork();

    if( pid != 0) exit(0);
      
    //second children
    chdir ("/");

    for (int i = 0; i < 3; i++) {
        close (i);
    }

    int stdfd = open ("/dev/null", O_RDWR);
    dup2(stdfd, STDOUT_FILENO);
    dup2(stdfd, STDERR_FILENO);

	return 0;
}



/*---------------------------------------------------------------------------------------
連線有出現變化會呼叫
這裡當有客戶端離線就會進來
---------------------------------------------------------------------------------------*/
static void
conn_eventcb(struct bufferevent *bev, short events, void *user_data)
{
	LOGI("Connection closed.\n");
	UserDisconnect((ntvuser *)user_data);
}
/*---------------------------------------------------------------------------------------
* 當有資料進來的時候會呼叫
* 使用 bufferevent_read 可以讀出資料
* 這個範例使用 bufferevent_write 回一些資料給 client
*
---------------------------------------------------------------------------------------*/
void conn_readcb(struct bufferevent *bev, void *ptr)
{

	ntvuser_t *pUser = (ntvuser_t *)ptr;
	char adbcommand[256];
	char tmp[1024];
	size_t n;
	tmp[1023] = 0;
	while (1) {
 		n = bufferevent_read(bev, tmp, sizeof(tmp)-1);

		if (n <= 0)
			break; /* No more data. */

		tmp[n] = 0;
		LOGI("recv data: %s ", tmp);
		
		if(strncmp(tmp, "login:NTV_Key:", 14) == 0) {
			if(pUser->login == 0) {
				char key[256];
				sscanf(tmp+6,"%s", key);
				if(strcmp("NTV_Key:no_key", NTV_Key) == 0 || strcmp(key, NTV_Key) == 0) 
				{
					pUser->login = 1;
					bufferevent_write(pUser->bev, tsheader, 188*4);
					continue;					
				}
			}
			UserDisconnect(pUser);
			return;
		} else if(strncmp(tmp, "key:", 4) == 0) {
			if(pUser->login)
				g_pRC->send(tmp, strlen(tmp));
			else {
				UserDisconnect(pUser);
				return;
			}
		} else if(strncmp(tmp, "cmd:", 4) == 0) {
			
			if(pUser->login) {
				if(strncmp(tmp+4, "stop", 4) == 0) {
					system("am broadcast -a tw.ironThomas.ntvserver.ACTION_FINISH");
					system("am stopservice -n tw.ironThomas.ntvserver/tw.ironThomas.ntvserver.StreamService");
				} else if(strncmp(tmp+4, "reboot", 6) == 0) {
					system("reboot");
				} else if(strncmp(tmp+4, "qt_fhd", 6) == 0) {
					system("am broadcast -a tw.ironThomas.ntvserver.ACTION_FINISH");
					system("am start -n tw.ironThomas.ntvserver/tw.ironThomas.ntvserver.MainActivity -e quality fullhd");
				} else if(strncmp(tmp+4, "qt_hd", 5) == 0) {
					system("am broadcast -a tw.ironThomas.ntvserver.ACTION_FINISH");
					system("am start -n tw.ironThomas.ntvserver/tw.ironThomas.ntvserver.MainActivity -e quality hd");
				} else if(strncmp(tmp+4, "qt_dvd", 6) == 0) {
					system("am broadcast -a tw.ironThomas.ntvserver.ACTION_FINISH");
					system("am start -n tw.ironThomas.ntvserver/tw.ironThomas.ntvserver.MainActivity -e quality dvd");
				}
				
			}
			else {
				UserDisconnect(pUser);
				return;
			}
		}
	}

}


void conn_localhostcb(struct bufferevent *bev, void *user)
{

	char tmp[1024];
	size_t n;
	tmp[1023] = 0;
	char *ptr;
	char param[64];

	n = bufferevent_read(bev, tmp, sizeof(tmp)-1);

	if (n > 0) {
	
		tmp[n] = 0;
		
		if((ptr = strstr(tmp, "quality:")) != NULL) {
			sscanf(ptr+8, "%s", param);
			
			if(strcmp(param, "1920x1080") == 0) {
				tsheader = ts_1920_1080;
				bufferevent_write(bev, "ok", 3);
				LOGI("set ts header: 1920x1080");
			} else if(strcmp(param, "1280x720") == 0) {
				tsheader = ts_1280_720;
				bufferevent_write(bev, "ok", 3);
				LOGI("set ts header: 1280x720");
			} else if(strcmp(param, "720x480") == 0) {
				tsheader = ts_720_480;
				bufferevent_write(bev, "ok", 3);
				LOGI("set ts header: 720x480");
			}

		}
		
		if((ptr = strstr(tmp, "NTV_Key:")) != NULL) {
			sscanf(ptr, "%s", NTV_Key);
			bufferevent_write(bev, "ok", 3);
			LOGI("%s", NTV_Key);
		}
		
		if((ptr = strstr(tmp, "cmd:")) != NULL) {
			sscanf(ptr+4, "%s", param);

			if(strcmp(param, "shutdown") == 0) {
				shutdown(g_stream_socket, 2);
				LOGI("command:shutdown");
			}
			
		}

	}

}




/*---------------------------------------------------------------------------------------
* 新客戶端連線觸發
* 這裡為新的客戶端配置一個 bufferevent 並設定 讀取 與 寫入完畢 與 離線的callback
---------------------------------------------------------------------------------------*/
static void
listener_cb(struct evconnlistener *listener, evutil_socket_t fd,
	struct sockaddr *sa, int socklen, void *user_data)
{
	struct event_base *base = (struct event_base *)user_data;
	struct bufferevent *bev;

	bev = bufferevent_socket_new(base, fd, BEV_OPT_CLOSE_ON_FREE);

	if (!bev) {
		LOGI("Error constructing bufferevent!");
		return;
	}

	sockaddr_in *si = (sockaddr_in *)sa;
	if (si->sin_family == AF_INET)
	{
		if((si->sin_addr.s_addr&0xFF) == 127 &&
			((si->sin_addr.s_addr&0xFF00)>>8) == 0 &&
			((si->sin_addr.s_addr&0xFF0000))>>16 == 0 &&
			((si->sin_addr.s_addr&0xFF000000)>>24) == 1 ) {
				bufferevent_setcb(bev, conn_localhostcb, NULL, NULL, NULL);	
				bufferevent_enable(bev, EV_READ| EV_WRITE);	
				return;
			}
  	}

	ntvuser_t *pUser;
	if((pUser = UserConnect(bev)) != NULL) {
		bufferevent_enable(bev, EV_READ| EV_WRITE);	
		bufferevent_setcb(bev, conn_readcb, NULL, conn_eventcb, pUser);		
		LOGI("on connect ");
		
	} else {
		bufferevent_free(bev);
	}

}
/*---------------------------------------------------------------------------------------
* 
* 
---------------------------------------------------------------------------------------*/
static void
timeout_cb(evutil_socket_t fd, short event, void *arg)
{
	struct event *timeout = (struct event *)arg;
	int qc;
	int packetSize = 0;
	ntvuser_t *pUser;
	

	x_lock_lock(&g_queue_lock);
	qc = x_queue_count(g_data_queue);
	if(qc >= 1316) {
		packetSize = (qc/1316)*1316;
		x_queue_read(g_data_queue, tmpbuf, packetSize);
	}		
	x_lock_unlock(&g_queue_lock);

	if(qc < 1316) {
		return;
	}
	
	for (int x = 0; x < max_user; x++) {
		pUser = &user_array[x];
		if (pUser->uses == 1 && pUser->login == 1) {

			int idx = 0;
			int ps = packetSize;
			while(ps > 0) {
				bufferevent_write(pUser->bev, tmpbuf+idx, 1316);
				ps -= 1316;
				idx += 1316;		
			}			
			
		}
	}	


}
/*---------------------------------------------------------------------------------------
* 
* 
---------------------------------------------------------------------------------------*/
x_thread_proc(UserConnectThread, void *nouse)
{
	struct sockaddr_in sin;
	struct evconnlistener *listener;
	
	evthread_use_pthreads();

    g_base = event_base_new();
	if (!g_base) {
		LOGI("Could not initialize libevent!");
		g_base = NULL;
		return 0;
	}
	
	memset(&sin, 0, sizeof(sin));
	sin.sin_family = AF_INET;
	sin.sin_port = htons(SERVER_PORT);

	
	listener = evconnlistener_new_bind(g_base, listener_cb, (void *)g_base,
		LEV_OPT_REUSEABLE | LEV_OPT_CLOSE_ON_FREE, -1,
		(struct sockaddr*)&sin,
		sizeof(sin));

		
	if (!listener) {
		LOGI("Could not create a listener!");
		return 0;
	}
	
	LOGI("start listen...");  
	
	static struct event timeout;
	struct timeval tv;

			
	/* Initalize one event */
	event_assign(&timeout, g_base, -1, EV_PERSIST, timeout_cb, (void*) &timeout);

	evutil_timerclear(&tv);
	tv.tv_sec = 0;
	tv.tv_usec = 1000*5;
	event_add(&timeout, &tv);

	event_base_dispatch(g_base);	

}




int main(int argc, char *argv[])
{
	init_daemon();
	
	pthread_t		thread_id;
	int				error_code;

	memset(user_array, 0, sizeof(user_array));
	

	x_lock_init(&g_queue_lock);
	g_data_queue = x_queue_alloc(queueSize);
	
	// 
	g_pRC = new RCClient;


	x_thread	MyThread;
	x_thread_start(&MyThread, UserConnectThread, NULL);
	
	
	
    if ( (g_stream_socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0)
    {
        LOGI("socket() failed");
        exit(EXIT_FAILURE);
    }
	
	int reuse	= 1;
	setsockopt(g_stream_socket, SOL_SOCKET, SO_REUSEADDR, (const char *)&reuse, sizeof(reuse));
	
	
	int buffsize = 512*1024; // 512 K
	setsockopt(g_stream_socket, SOL_SOCKET, SO_RCVBUF, (void*)&buffsize, sizeof(buffsize));

	
	
	struct sockaddr_in server;
	struct sockaddr_in si_other;
	
	//Prepare the sockaddr_in structure
	memset(&server, 0, sizeof(struct sockaddr_in));
	server.sin_family			= AF_INET;
	server.sin_addr.s_addr	= htonl(INADDR_ANY);
	server.sin_port			= htons(TS_PORT);


	if( bind(g_stream_socket ,(struct sockaddr *)&server , sizeof(server)) < 0)
	{
		LOGI("Bind failed");
		exit(EXIT_FAILURE);
	}
	
	char buf[1024*8];
	int recv_len;
	int slen = sizeof(si_other);
	

	while(1) {

		if ((recv_len = recvfrom(g_stream_socket, buf, sizeof(buf), 0, (struct sockaddr *) &si_other, &slen)) <= 0) {
			LOGI("recvfrom() failed");
			break;
		}
		else {
			x_lock_lock(&g_queue_lock);
			if(x_queue_write(g_data_queue, (unsigned char *)buf, recv_len) == 0) {
				LOGI("Buffer overflow");
			}
			x_lock_unlock(&g_queue_lock);
		}
	}	
	
	
	delete g_pRC;
	x_queue_free(g_data_queue);
	
	return 0;
}
