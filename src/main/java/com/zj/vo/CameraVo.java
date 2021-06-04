package com.zj.vo;

import lombok.Data;

/**
 * 
 * @author ZJ
 *
 */
@Data
public class CameraVo {

	private String url;
	private String remark;
	private boolean enabledFlv = false;
	private boolean enabledHls = false;
	
	private String mode = "未开启";
}
