//
// Created by wangqiang on 16/8/20.
//

#ifndef SNIFFER_MAIL_H
#define SNIFFER_MAIL_H
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <errno.h>
#include <unistd.h>
#include <sys/time.h>
#include <netdb.h>

class MailSender {
public:
    MailSender();
    ~MailSender();
    /**
     * 发送邮件
     * subject:邮件主题
     * content:邮件内容
     * return:0表示成功,否则为错误码
     */
    int send_mail(const char *subject, const char *content);
private:
    bool init_addr();
    bool connect_smtp_server();
    int base64_encode(unsigned char *buf, int nLen, char *pOutBuf, int nBufSize);
    const char *make_body(const char *subject, const char *content);
    int check_success(const char *rsp);

    int m_conn_fd;
    struct sockaddr_in m_addr_in;
};

#endif //SNIFFER_MAIL_H
