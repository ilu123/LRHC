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

char*       serverIP= "10.42.0.1";
int         serverPort=4100;  // this port is for RGBD camera stream

int main(int argc, char** argv)
{

    //--------------------------------------------------------
    //networking stuff: socket , connect
    //--------------------------------------------------------



    string str = argv [1] ;

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



    //----------------------------------------------------------
    //OpenCV Code
    //----------------------------------------------------------



	string pointCloudMap = argv [2] ;  //此处读取下载下来的点云地图

	cv::Mat pc = cv::imread (pointCloudMap , CV_8UC1 ) ; 
	
	
	

      
	// 点云地图的宽与高
	int pw = pc.cols ;
	int ph = pc.rows ; 


	cv::Mat white =  cv::Mat(ph, pw, CV_8UC1, cv::Scalar(255)) ; 

	pc = white - pc ; //图像反向， 黑->白 ， 白->黑

	

	
	


	int bytes;

	//获取地图宽度
	int mapWidth =0 ;	// width of the map
	if ((bytes = recv(sokt, &mapWidth, sizeof (mapWidth), MSG_WAITALL)) <= 0){
             std::cerr << "bytes = " << bytes << std::endl;
	     return 0;

        }

	//cout << " map width " << mapWidth << endl;

	//获取地图高度
	int mapHeight =0 ;	// height of the map
	if ((bytes = recv(sokt, &mapHeight, sizeof (mapHeight), MSG_WAITALL)) <= 0){
             std::cerr << "bytes = " << bytes << std::endl;
	     return 0;
        }


	cout << " map width " << mapHeight << endl;

	cv::Mat lasermap = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0));

	cv::Mat pose = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0));


	cv::Mat trajectory = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0));



	if ( ! ( pw == mapWidth && ph == mapHeight) ) { //地图尺寸不一样的，有可能是特征点地图输入错误，退出程序

		cout << pw << mapWidth << ph << mapHeight << endl;

		cout << " map size unmatch " << endl; 

		return -1;

	}


	cv::Mat covisibleMap = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0)) ;   //共试图，用于保存特征点和激光同时能看到的静态障碍


	cv::Mat laserPoint  = cv::Mat(mapHeight, mapWidth, CV_8UC1, cv::Scalar(0)) ;    // 用于画每针看到的激光点
	



	int key = -1  ;


    while ((char)key!= 'q') {


	int sendMsg = 0 ;




	//获取x坐标
	int x_cam =0 ;	 
	if ((bytes = recv(sokt, &x_cam, sizeof (x_cam), MSG_WAITALL)) <= 0){
             std::cerr << "bytes = " << bytes << std::endl;
	     return 0;
        }

	//获取y坐标
	int y_cam =0 ;	 
	if ((bytes = recv(sokt, &y_cam, sizeof (y_cam), MSG_WAITALL)) <= 0){
             std::cerr << "bytes = " << bytes << std::endl;
	     return 0;
        }




	std::vector< std::pair< int , int> > laser_dots;
	int sz=0;

	//获取dot尺寸
	if ((bytes = recv(sokt, &sz, sizeof(sz) , MSG_WAITALL)) == -1) {
	    std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	}
	//cout << "laser dot size" << sz <<endl;

	//获取点云
	laser_dots.resize( sz );
	if ((bytes = recv(sokt, &(*laser_dots.begin()), sz * 8  , MSG_WAITALL)) == -1) {
	    std::cerr << "recv failed, received bytes = " << bytes << std::endl;
	}



 	laserPoint.setTo ( cv::Scalar (0)) ;


	for(int i=0; i<laser_dots.size(); i++)
    	{
	 int x_p = laser_dots[i].first;
	 int y_p = laser_dots[i].second;
	   



	 
	 if(x_p >= 0 && y_p >=0 && x_p < lasermap.cols && y_p < lasermap.rows)
	  {

	     //lasermap 画布 画激光线
	     cv::line(lasermap,cv::Point(x_cam,y_cam),cv::Point(x_p,y_p),cv::Scalar(255));
	     lasermap.at<uchar>(y_p,x_p) = 0;


	     //laserdot画布 画激光点
	     cv::circle ( laserPoint , cv::Point ( x_p , y_p) ,  2 , cv::Scalar(255) , -1 ) ; 
	     lasermap.at<uchar>(y_p,x_p) = 0;
	

	  }
	}


	
	cv::Mat res;
	bitwise_and(laserPoint,pc,res); // laserpoint 图层和 pointcloud 图层 按位与运算。
	covisibleMap = covisibleMap + res ; // 结果添加到共视图上

	
	//当前位置图层
	cv::circle ( pose , cv::Point ( x_cam , y_cam) ,  4 , cv::Scalar(255) , -1 ) ; 
	//轨迹图层
	cv::circle ( trajectory , cv::Point ( x_cam , y_cam) ,  8 , cv::Scalar(175) , -1 ) ; 


	//最终地图图层
	cv::Mat finalMap  ; 

	
	//将轨迹经过的位置都涂成可通行区域
	bitwise_or( lasermap - covisibleMap,trajectory*10,finalMap);


	
	//lasermap = lasermap  + trajectory*10 ;

  
  
	cv::imshow(" finalMap", finalMap - trajectory -pose);

	key = cv::waitKey(1);

	pose.setTo( cv::Scalar ( 0)) ;


	if ( (char)key == 's' ) {
		cv::imwrite ( "currentMap.pgm" , finalMap - trajectory ) ;  // 保存叠加后的最终地图和轨迹图层
		cv::imwrite ( "currentMap_laser.pgm" , lasermap - trajectory ) ;
		cout << "save map " << endl ;

	}else if ( (char)key == 'r' ) {

		lasermap.setTo ( cv::Scalar ( 0)  ) ; 
		cout << "reset" << endl;
	}


 

    }

    close(sokt);

    return 0;
}
