//
// Created by wangqiang on 16/8/20.
//
#include <mail/mail.h>
#include "native/com_jupiter_mail_MailInterface.h"

JNIEXPORT jint JNICALL
Java_com_jupiter_mail_MailInterface_sendMail(JNIEnv *env, jobject instance, jstring subject_,
jstring content_) {
    const char *subject = env->GetStringUTFChars(subject_, 0);
    const char *content = env->GetStringUTFChars(content_, 0);

    MailSender sender = MailSender();
    int ret = sender.send_mail(subject, content);

    env->ReleaseStringUTFChars(subject_, subject);
    env->ReleaseStringUTFChars(content_, content);

    return ret;
}