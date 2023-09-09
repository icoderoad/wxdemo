package com.icoderoad.example.userversion.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.icoderoad.example.userversion.entity.User;
import com.icoderoad.example.userversion.entity.UserVersion;
import com.icoderoad.example.userversion.interceptor.UserChangeInterceptor;
import com.icoderoad.example.userversion.repository.UserRepository;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

@Controller
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserChangeInterceptor userChangeInterceptor;

	@GetMapping("/")
	public String userList(Model model) {
		List<User> users = userRepository.findAll();
		model.addAttribute("users", users);
		return "user/users";
	}

	@GetMapping("/add")
	public String addUserForm(Model model) {
		model.addAttribute("user", new User());
		return "user/add";
	}

	@PostMapping("/save")
	public String saveUser(@ModelAttribute User user) {
		// 保存用户信息到MongoDB
		createUserVersion(user);
		userRepository.save(user);
		return "redirect:/";
	}

	@GetMapping("/edit/{id}")
	public String editUserForm(@PathVariable String id, Model model) {
		User user = userRepository.findById(id).orElse(new User());
		model.addAttribute("user", user);
		return "user/edit";
	}

	@PostMapping("/update")
	public String updateUser(@RequestParam String id, @ModelAttribute User user) {
		user.set_id(id);
		User mUser = userRepository.findById(id).orElse(new User());
		user.setVersionHistory(mUser.getVersionHistory());
		// 更新用户信息到MongoDB
		createUserVersion(user);
		userRepository.save(user);
		return "redirect:/";
	}

	@GetMapping("/compare")
	public String compareUsersForm(Model model) {
		List<User> users = userRepository.findAll();
		model.addAttribute("users", users);
		return "user/compare";
	}
	 @GetMapping("/compareVersions")
	    public String compareVersions(
	            @RequestParam String id,
	            @RequestParam(required = false) Integer version1Index,
	            @RequestParam(required = false) Integer version2Index,
	            Model model) {

	        // 获取用户和版本信息
	        User user = userRepository.findById(id).orElse(null);
	        model.addAttribute("userId", id);
	        if (user != null) {
	            List<UserVersion> versions = user.getVersionHistory();
	            model.addAttribute("userVersions", versions);
	            if( version1Index == null ) {
	            	version1Index = -1;
	            }
	            if( version2Index == null ) {
	            	version2Index = -1;
	            }
	            if (version1Index >= 0 && version1Index < versions.size()
	                    && version2Index >= 0 && version2Index < versions.size()) {
	                UserVersion version1 = versions.get(version1Index);
	                UserVersion version2 = versions.get(version2Index);

	                // 执行文本差异比较
	                String usernameDiff = compareStrings(version1.getUsername(), version2.getUsername());
	                String emailDiff = compareStrings(version1.getEmail(), version2.getEmail());

	                // 将比较结果存储在model中，以便在比较结果页面显示
	                model.addAttribute("usernameDiff", usernameDiff);
	                model.addAttribute("emailDiff", emailDiff);
	                model.addAttribute("comparisonResult", true);

	            }
	            
	            return "user/compareVersions"; // 返回比较结果页面
	        }

	        return "redirect:/users"; // 如果未找到用户或版本，重定向到用户列表页面
	    }

	    private String compareStrings(String str1, String str2) {
	        List<String> original = Arrays.asList(str1.split("\\n")); // 将字符串拆分为行
	        List<String> revised = Arrays.asList(str2.split("\\n")); // 将字符串拆分为行

	        // 创建Patch对象来存储差异
	        Patch<String> patch = DiffUtils.diff(original, revised);

	        StringBuilder diffOutput = new StringBuilder();
	        for (Delta<String> delta : patch.getDeltas()) {
	            String originalText = delta.getOriginal().getLines().toString();
	            String revisedText = delta.getRevised().getLines().toString();

	            // 添加高亮样式以区分新增和删除部分
	            diffOutput.append("<span class=\"deleted\">").append(originalText).append("</span>\n");
	            diffOutput.append("<span class=\"added\">").append(revisedText).append("</span>\n");
	        }

	        return diffOutput.toString();
	    }
	    
	    private boolean isUserInfoChanged(User user, UserVersion version) {
	        // 在这里添加逻辑来比较用户信息是否发生更改
	        return !user.getUsername().equals(version.getUsername())
	            || !user.getEmail().equals(version.getEmail())
	           ;
	    }
	    
	    private void createUserVersion(User user) {
	        // 获取用户的版本历史
	        List<UserVersion> versions = user.getVersionHistory();

	        if (versions.isEmpty() || isUserInfoChanged(user, versions.get(versions.size() - 1))) {
	            // 仅在用户信息发生更改或无版本历史记录时创建新的用户版本
	            UserVersion newUserVersion = new UserVersion();
	            newUserVersion.setUsername(user.getUsername());
	            newUserVersion.setEmail(user.getEmail());
	            // 设置其他版本信息字段

	            // 将新版本添加到用户的版本历史
	            versions.add(newUserVersion);

	            // 保存用户对象以更新版本历史
	            userRepository.save(user);
	        }
	    }
}