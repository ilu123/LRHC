/**
 * 获取所有地图
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
int         serverPort=4101;  // 该端口用来传输地图

int main(int argc, char** argv)
{

    string str = slamBoardIP(); //获取ip

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

	int signal = 2017 ;    

	/* 
	signal meaning:
	2017 : retrive all maps from SLAM borad  获取所有地图
	2018  : send an edited map to SLAM board and replace the original one  发送一张修改过的地图给机器人
	*/
	
	// 发送 信号
        if ((bytes = send(sokt, &signal, sizeof (signal) , 0 )) < 0 ) {
            std::cerr << "send failed, send bytes = " << bytes << std::endl;
        }else {
		cout << "sent " << signal << endl; 
	}
        

	// 获取所有地图
	if (signal == 2017) { 


		int mapCnt =0 ; 
		//获取地图总数量				
		if ((bytes = recv(sokt, &mapCnt, sizeof(mapCnt) , MSG_WAITALL)) <=0) {
    			std::cerr << "recv failed, received bytes = " << bytes << std::endl;

			return 0;
		}

		cout << " number of maps : " << mapCnt << endl;

		
		for ( int i = 0 ; i < mapCnt ; i++ ){

			//获取地图名称长度【字符个数】
			int lenMapName = 0 ; // size of the mapName
			if ((bytes = recv(sokt, &lenMapName, sizeof (lenMapName), MSG_WAITALL)) <= 0){
                             std::cerr << "bytes = " << bytes << std::endl;
			     return 0;                        
                        }

                        char mapNameBuff [1024] ;  // name of the map

			//获取地图名称
                        if ((bytes = recv(sokt, mapNameBuff, lenMapName, 0)) <= 0){
                             std::cerr << "bytes = " << bytes << std::endl;
			     return 0;
                             
                        }
			string mapName = string(mapNameBuff).substr(0 , bytes) ; 

			cout << " Name of map : " << mapName << endl;

			//获取地图宽度
			int mapWidth =0 ;	// width of the map
			if ((bytes = recv(sokt, &mapWidth, sizeof (mapWidth), MSG_WAITALL)) <= 0){
                             std::cerr << "bytes = " << bytes << std::endl;
			     return 0;
                             
                        }

			cout << " map width " << mapWidth << endl; 
		
			//获取地图高度
			int mapHeight =0 ;	// height of the map
			if ((bytes = recv(sokt, &mapHeight, sizeof (mapHeight), MSG_WAITALL)) <= 0){
                             std::cerr << "bytes = " << bytes << std::endl;
			     return 0;                             
                        }			


			cout << " map height " << mapHeight << endl; 

			//制作地图容器
			Mat img;  // image container
			img = Mat::zeros( mapHeight ,  mapWidth , CV_8UC1);    // CV_8UC1 means that each pixels in the image is type unsigned char  
			int imgSize = img.total() * img.elemSize();  
			uchar *iptr = img.data; 

			//获取地图数据本身
			cout << "imgSize " << imgSize<< endl;
			if ((bytes = recv(sokt, iptr, imgSize , MSG_WAITALL))  <=0 ) {
			     std::cerr << "recv failed, received bytes = " << bytes << std::endl;
			     return 0;                             
			}

			//保存到本地
			cv::imwrite (  string(mapName) + ".pgm",  img  );
			

		}


	}





    close(sokt);

    delete [] serverIP ; 
    return 0;
}	
