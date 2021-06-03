package com.zj.entity;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

/**
 * camera相机
 * @author ZJ
 *
 */
@Getter
@Setter
public class Camera implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5575352151805386129L;
	
	private String id;
	
	/**
	 * rtsp、rtmp、d:/flv/test.mp4、desktop
	 */
	private String url;
	
	/**
	 * 流备注
	 */
	private String remark;
	
	/**
	 * flv开启状态
	 */
	private boolean status;
	
	/**
	 * hls开启状态
	 */
	private boolean hlsStatus;
	
	/**
	 * md5 key，媒体标识
	 */
	private String mediaKey;
	
	/**
	 * 0网络流，1本地视频
	 */
	private int type = 0;
}
