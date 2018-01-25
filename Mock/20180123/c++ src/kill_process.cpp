/**
 * HCLR SLAM Product interface demo
 */


#include "opencv2/opencv.hpp"
#include <sys/socket.h> 
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>
#include "utilities.h"
using namespace std;
using namespace cv;


char*       serverIP= "10.42.0.1";
int         serverPort=4102;  // this port is for psa 

int main(int argc, char** argv)
{


    string str = slamBoardIP();

    cout << " server IP:  " << str << endl;

   serverIP = new char [ str.size() + 1  ] ; 

    serverIP[str.size()]= '\0' ; 

    std::copy(str.begin(), str.end(), serverIP) ;





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


	int bytes = 0 ;

	int signal =2019 ; // kill a process with its Name  	
 
	if ((bytes = send(sokt,  &signal,  4 , 0)) == -1 ){
		
                std::cout << "send error !" << endl;
		return 0 ;

	} 


	string processName = argv [1]  ;  // process name
              
	if ((bytes = send(sokt, processName.c_str(), processName.size(), 0)) == -1 ){                             
                          
		std::cout << "send error !" << endl;

		return 0;
                   
	}


	char resuBuff  [ 1024 ]  ; 

        if ((bytes = recv(sokt, resuBuff , 1024 ,  0)) == -1 ){

                std::cout << "recv error !" << endl;
                return 0;

        }


		


	cout << "recv bytes " << bytes << endl;
	cout << "ps  -aux | grep "+ processName << endl;
	cout <<  resuBuff  << endl;

    	close(sokt);
	
	delete  [] serverIP  ; 

    	return 0;

}	
