#define LOG_TAG "UDPStream"
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
#include <errno.h>

#include <android/log.h>


#include <jni.h>


#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native-UDPFileDescriptor", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "native-UDPFileDescriptor", __VA_ARGS__))





extern "C" {
    JNIEXPORT jobject JNICALL Java_tw_ironThomas_ntvserver_UDPFileDescriptor_open(JNIEnv * env, jobject clazz, jstring SERVER, jint PORT, jint cache_size);
    JNIEXPORT void JNICALL Java_tw_ironThomas_ntvserver_UDPFileDescriptor_close(JNIEnv * env, jobject clazz, jobject fileDescriptor);
};




int jniThrowException(JNIEnv *env, const char* msg) {
    jclass exceptionClass = env->FindClass("java/io/IOException");
    if (exceptionClass == NULL) {
        return -1;
    }
    if (env->ThrowNew(exceptionClass, msg) != JNI_OK) {

    }
    return 0;
}




jobject jniCreateFileDescriptor(JNIEnv *env, int fd)
{
	jclass cFileDescriptor = env->FindClass("java/io/FileDescriptor");
	jfieldID descriptorID = env->GetFieldID(cFileDescriptor,"descriptor","I");
	jmethodID iFileDescriptor = env->GetMethodID(cFileDescriptor,"<init>","()V");
	
	jobject mFileDescriptor = env->NewObject(cFileDescriptor, iFileDescriptor);
	env->SetIntField(mFileDescriptor, descriptorID, (jint)fd);
	return mFileDescriptor;
}


JNIEXPORT jobject Java_tw_ironThomas_ntvserver_UDPFileDescriptor_open(JNIEnv* env, jobject clazz, jstring SERVER, jint PORT, jint cache_size)
{
	char serverip[64];
	const char* ip = (SERVER ? env->GetStringUTFChars(SERVER, NULL) : NULL);
    if (ip) {
		strcpy(serverip, ip);
        env->ReleaseStringUTFChars(SERVER, ip);
	}
	else {
		return NULL;
	}
	
	struct sockaddr_in addr;
    int s;
	int reuseFlag	= 1;
	
    //create socket
    if ( (s=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0)
    {
		jniThrowException(env, "socket() failed");
        return NULL;
    }

	setsockopt(s, SOL_SOCKET, SO_REUSEADDR, (const char *)&reuseFlag, sizeof(reuseFlag));
	
	
	int buffsize = cache_size; // 512 K
	setsockopt(s, SOL_SOCKET, SO_SNDBUF, (void*)&buffsize, sizeof(buffsize));
	
	
    //setup address structure
    memset((char *) &addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(PORT);
    addr.sin_addr.s_addr = inet_addr(serverip);
	
	if(connect(s, (struct sockaddr *)&addr, sizeof(addr)) == -1) {
        jniThrowException(env, "connect failed");
        return NULL;		
	}
	LOGI("udp socket open: %d \n", s);
    return jniCreateFileDescriptor(env, s);
}


JNIEXPORT void Java_tw_ironThomas_ntvserver_UDPFileDescriptor_close(JNIEnv* env, jobject clazz, jobject fileDescriptor)
{
	int fd;
	jclass cFileDescriptor = env->FindClass("java/io/FileDescriptor");
	jfieldID descriptorID = env->GetFieldID(cFileDescriptor,"descriptor","I");
	fd = env->GetIntField(fileDescriptor, descriptorID);
	if(fd >= 0) {
		LOGI("udp socket close: %d \n", fd);
		env->SetIntField(fileDescriptor, descriptorID, -1);
		close(fd);
	}

}
