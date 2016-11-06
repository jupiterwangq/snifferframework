#include <errno.h>
#include <mail/mail.h>
#include "../../include/native/packetreceiver.h"
#include "../../include/native/jnicontext.h"
#include "native/com_jupiter_snifferframework_Sniffer.h"
#include "../../include/log.h"

JavaVM *g_jvm = NULL;
JNIEnv *g_env = NULL;

void generate_path(const char* daemonPath) {
	int len = strlen(daemonPath);
	strncpy(Path::sema_put_file_path,   daemonPath, len);
	strncpy(Path::sema_get_file_path,   daemonPath, len);
	strncpy(Path::sema_mutex_file_path, daemonPath, len);
	strncpy(Path::shm_file_path,        daemonPath, len);

	strncat(Path::sema_put_file_path + len, Path::PUT, strlen(Path::PUT));
	strncat(Path::sema_get_file_path + len, Path::GET, strlen(Path::GET));
	strncat(Path::sema_mutex_file_path + len, Path::MUTEX, strlen(Path::MUTEX));
	strncat(Path::shm_file_path + len, Path::SHM, strlen(Path::SHM));
}

JNIEXPORT jint JNICALL Java_com_jupiter_snifferframework_Sniffer_nativeSniff
  (JNIEnv *env, jobject jsniffer, jstring daemonPath) {
	JNIContext::get()->init(jsniffer, env->GetObjectClass(jsniffer));
	generate_path(JNIContext::c_str(daemonPath));
	PacketReceiver::get()->start_receive();
}

JNIEXPORT void JNICALL Java_com_jupiter_snifferframework_Sniffer_nativeStop
  (JNIEnv *env, jobject jsniffer) {
	PacketReceiver::get()->stop_receive();
}

jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved ) {
	JNIEnv *env;

	if ( jvm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK) {
		return -1;
	}

    LOGE("JNI_OnLoad ok");

	g_jvm = jvm;
	g_env = env;

	return JNI_VERSION_1_4;
}