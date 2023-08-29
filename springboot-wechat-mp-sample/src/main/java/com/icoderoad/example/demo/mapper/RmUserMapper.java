package com.icoderoad.example.demo.mapper;

import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.icoderoad.example.demo.entity.RmUser;

@Repository
public interface RmUserMapper extends BaseMapper<RmUser> {
	
	@Select({
		"<script>",
		"SELECT "
		+" * "
		+ " from rm_user WHERE `username`=#{arg0} ",
		"</script>"
	})
	RmUser findByUsername(String username);
}