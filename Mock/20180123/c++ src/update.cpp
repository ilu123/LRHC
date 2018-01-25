/**
 *  
 */


#include "opencv2/opencv.hpp"
#include <sys/socket.h> 
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>
#include "utilities.h"
using namespace std;
using namespace cv;


char*       serverIP;
int         serverPort=4300;  // this port is for 更新 

int main(int argc, char** argv)
{

    //--------------------------------------------------------
    //networking stuff: socket , connect
    //--------------------------------------------------------


    string str = slamBoardIP();

    cout << " server IP:  " << str << endl;

   serverIP = new char [ str.size() + 1  ] ; 

    serverIP[str.size()]= '\0' ; 

    std::copy(str.begin(), str.end(), serverIP) ;






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

 




	// 更新文件   文件名   路径  网络地址 
	string fileName = "version.txt" ;

	string filePath = "/home/hclr/Desktop"; 

	string inetPath = "http://test-1255413295.cosbj.myqcloud.com/version.txt";

  

	int bytes = 0 ; 



	int signal = 2017 ; // 更新文件
	if ((bytes = send(sokt, &signal, sizeof (signal), MSG_WAITALL)) <= 0){
	     std::cerr << "send error bytes = " << bytes << std::endl;
	     return 0;                        
	}


	int lenFileName = fileName.size() ; // size of 文件名
	if ((bytes = send(sokt, &lenFileName, sizeof (lenFileName), MSG_WAITALL)) <= 0){
	     std::cerr << "send error bytes = " << bytes << std::endl;
	     return 0;                        
	}

	 
	if ((bytes = send(sokt, fileName.c_str(), lenFileName, MSG_WAITALL)) <= 0){
	     std::cerr << "send error bytes = " << bytes << std::endl;
	     return 0;                        
	}





	int lenFilePath = filePath.size() ; // size of 文件path
	if ((bytes = send(sokt, &lenFilePath, sizeof (lenFilePath), MSG_WAITALL)) <= 0){
	     std::cerr << "send error bytes = " << bytes << std::endl;
	     return 0;                        
	}

	 
	if ((bytes = send(sokt, filePath.c_str(), lenFilePath, MSG_WAITALL)) <= 0){
	     std::cerr << "send error bytes = " << bytes << std::endl;
	     return 0;                        
	}



	int lenInetPath = inetPath.size() ; // size of 文件 net
	if ((bytes = send(sokt, &lenInetPath, sizeof (lenInetPath), MSG_WAITALL)) <= 0){
	     std::cerr << "send error bytes = " << bytes << std::endl;
	     return 0;                        
	}

	 
	if ((bytes = send(sokt, inetPath.c_str(), lenInetPath, MSG_WAITALL)) <= 0){
	     std::cerr << "send error bytes = " << bytes << std::endl;
	     return 0;                        
	}




 







    close(sokt);

    delete  [] serverIP  ; 
    return 0;
}	
