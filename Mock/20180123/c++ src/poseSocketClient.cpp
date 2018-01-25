/**
 * HCLR SLAM Product interface demo
 */

#include "opencv2/opencv.hpp"
#include <sys/socket.h> 
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>

using namespace cv;
using namespace std;

char*       serverIP= "192.168.3.12";
int         serverPort=4098;  // this port is for robot pose

#define DEFAULT_BUFLEN 512

int main(int argc, char** argv)
{

    //--------------------------------------------------------
    //networking stuff: socket , connect
    //--------------------------------------------------------
    int         sokt;


    struct  sockaddr_in serverAddr;
    socklen_t           addrLen = sizeof(struct sockaddr_in);

    if ((sokt = socket(PF_INET, SOCK_STREAM, 0)) < 0) {
        std::cerr << "socket() failed" << std::endl;
    }

    serverAddr.sin_family = PF_INET;
    serverAddr.sin_addr.s_addr = inet_addr(serverIP);
    serverAddr.sin_port = htons(serverPort);

    if (connect(sokt, (sockaddr*)&serverAddr, addrLen) < 0) {
        std::cerr << "connect() failed!" << std::endl;
    }

 



    char recvbuf[DEFAULT_BUFLEN];

 

    while (true) {

	int bytes = 0 ;


        if ((bytes = recv(sokt, recvbuf, DEFAULT_BUFLEN , 0)) == -1) {
            std::cerr << "recv failed, received bytes = " << bytes << std::endl;
        }
        
        cout <<  "  recv buff "<<recvbuf << endl; 

   }


	void test() ; 
 
 

    close(sokt);

    return 0;
}	
