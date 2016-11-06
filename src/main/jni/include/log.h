#ifndef __LOG_H__
#define __LOG_H__

#include <android/log.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "Sniffer_native", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , "Sniffer_native", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  , "Sniffer_native", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN  , "Sniffer_native", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "Sniffer_native", __VA_ARGS__)

#endif