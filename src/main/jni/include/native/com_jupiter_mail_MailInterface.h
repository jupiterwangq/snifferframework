//
// Created by wangqiang on 16/8/20.
//
#include <jni.h>
#ifndef SNIFFER_COM_JUPITER_MAIL_MAILINTERFACE_H
#define SNIFFER_COM_JUPITER_MAIL_MAILINTERFACE_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * 返回0表示发送成功,否则为服务器的错误码
 */
JNIEXPORT jint JNICALL
Java_com_jupiter_mail_MailInterface_sendMail(JNIEnv *env, jobject instance, jstring subject_,
jstring content_);

#ifdef __cplusplus
}
#endif

#endif //SNIFFER_COM_TENCENT_MAIL_MAILINTERFACE_H
