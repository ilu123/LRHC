/**
 * HCLR SLAM Product interface demo
 * 发送修改过的地图
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
int         serverPort=4101;  // this port is for changing 

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

 
 

  

	int bytes = 0 ; 

	int signal = 2019 ; 

	/* 
	signal meaning:
	2017 : retrive all maps from SLAM borad
	2018  : send an edited map to SLAM board and replace the original one
	*/
	


	string mapName =  argv[1]  ; //修改过的地图的名字 
	//cv::Mat img = cv::imread( mapName + ".pgm" , CV_8UC1) ;  //读取本地修改过得地图


	



	if ((bytes = send(sokt, &signal, sizeof (signal) , 0 )) < 0 ) {
            std::cerr << "send failed, send bytes = " << bytes << std::endl;
        }else {
		cout << "sent " << signal << endl; 
	}
        


	if (signal == 2019) { 

		// send the size of  mapName ; 发送地图名称在所占字节个数
		int mapNameLen = mapName.size() ;
		if ((bytes = send(sokt, &mapNameLen,  sizeof(mapNameLen) , 0)) <= 0){
	             std::cerr << "bytes = " << bytes << std::endl;
	             return 0;
	        }

		// send the name of the editedMap  发送地图名称
		if ((bytes = send(sokt, mapName.c_str(), mapName.size(), 0)) <= 0){
	             std::cerr << "bytes = " << bytes << std::endl;
	             return 0;
	        }

		
		//recieve ack information 
		//获取反馈信息
                char ackBuff [1024] ;  
                if ((bytes = recv(sokt, ackBuff, 1024, 0)) <= 0){
                     std::cerr << "bytes = " << bytes << std::endl;
		     return 0;
                }
		cout << "SLAM board says: " <<  ackBuff  << endl;
		

		
		
	}





    close(sokt);

    delete  [] serverIP  ; 
    return 0;
}	
