package com.zj.dto;

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
	private boolean enabledFlv = true;
	
	/**
	 * hls开启状态
	 */
	private boolean enabledHls = false;
	
	/**
	 * 是否启用ffmpeg，启用ffmpeg则不用javacv
	 */
	private boolean enabledFFmpeg = false;
	
	/**
	 * 无人拉流观看是否自动关闭流
	 */
	private boolean autoClose;
	
	/**
	 * md5 key，媒体标识，区分不同媒体
	 */
	private String mediaKey;
	
	/**
	 * 网络超时 ffmpeg默认5秒，这里设置15秒
	 */
	private String netTimeout = "15000000";
	/**
	 * 读写超时，默认5秒
	 */
	private String readOrWriteTimeout = "15000000";

	/**
	 * 无人拉流观看持续多久自动关闭，默认1分钟
	 */
	private long noClientsDuration = 60000;
	
	/**
	 * 0网络流，1本地视频
	 */
	private int type = 0;
}
