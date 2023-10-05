$(document).ready(function () {
	    const frameNumberInput = $('#frameNumber');
	    const thumbnailContainer = $('#thumbnailContainer');
	    const thumbnailImage = $('#thumbnailImage');
	    const videoFileInput = $('#videoFile'); // 添加视频文件输入
			
	   	const generateThumbnailBtn = $('#generateThumbnailBtn');

        // 当用户点击生成指定帧缩略图按钮时触发
        generateThumbnailBtn.on('click', function () {
            const frameNumber = parseInt($('#frameNumber').val());
            const videoFile = $('#videoFile')[0].files[0];
            
            // 检查文件扩展名是否为mp4
            if (checkFileExtension(videoFile, 'mp4')) {
                generateThumbnail(frameNumber, videoFile);
            } else {
                alert('请上传MP4格式的视频文件。');
            }
        });

        // 生成指定帧缩略图的函数
        function generateThumbnail(frameNumber, videoFile) {
            // 向后端发送请求，生成指定帧的缩略图
            $.ajax({
                url: '/generateThumbnail', // 根据实际的后端处理路径修改
                type: 'POST',
                data: {
                    frameNumber: frameNumber,
                    videoFile: videoFile.name
                },
                success: function (data) {
                    if (data && data.thumbnailURL) {
                        // 显示生成的缩略图
                        $('#thumbnailImage').attr('src', data.thumbnailURL);
                        alert('缩略图生成成功！');
                    } else {
                        alert('缩略图生成失败。');
                    }
                },
                error: function () {
                    alert('缩略图生成失败。');
                }
            });
        }
	  
	  	// 检查文件扩展名的函数
	    function checkFileExtension(file, validExtension) {
	        const fileExtension = file.name.split('.').pop().toLowerCase();
	        return fileExtension === validExtension;
	    }
	  
	    //  初始化拖动功能，开始时禁用它
	    frameNumberInput.slider({
	        min: 1,
	        step: 1,
	        slide: function (event, ui) {
	            // 更新输入框的值
	            frameNumberInput.val(ui.value);
	        },
	      	disabled: true // 初始禁用
	    });

		// 当用户输入帧号时，更新拖动条
		frameNumberInput.on('input', function() {
			const frameNumber = parseInt(frameNumberInput.val());
			frameNumberInput.slider('value', frameNumber);
		});

	    // 当视频文件选择发生变化时
	    videoFileInput.on('change', function () {
	        const videoFile = videoFileInput[0].files[0];
	        if (videoFile) {
	            // 上传视频文件
	            uploadVideoFile(videoFile);
	        }
	    });

	    // 上传视频文件的函数
	    function uploadVideoFile(videoFile) {
	      	// 检查文件扩展名是否为mp4
	      	if (!checkFileExtension(videoFile, 'mp4')) {
	          alert('请上传MP4格式的视频文件。');
	          return;
	      	}
	
	        const formData = new FormData();
	        formData.append('file', videoFile);
	
	        $.ajax({
	            url: '/uploadVideo', // 根据实际接口地址修改
	            type: 'POST',
	            data: formData,
	            contentType: false,
	            processData: false,
	            success: function (data) {
	                if (data && data.success) {
	                    console.log('视频上传成功');
	                     // 从上传视频接口返回的data中获取videoFilePath
	                    const videoFilePath = data.videoFilePath;
	
	                    // 调用getTotalFrames并传递videoFilePath参数
	                    getTotalFrames(videoFilePath);
	                } else {
						alert(data.msg);
	                    console.error('视频上传失败');
	                }
	            },
	            error: function () {
	                console.error('视频上传失败');
	            }
	        });
	    }
	
	  	// 获取视频总帧数的函数
		function getTotalFrames(videoFilePath) {
		    $.ajax({
		        url: '/getTotalFrames',
		        type: 'GET',
		        data: { videoFilePath: videoFilePath }, // 传递videoFilePath参数
		        dataType: 'json',
		        success: function (data) {
		            if (data && data.totalFrames) {
		                const totalFrames = data.totalFrames;
		                frameNumberInput.slider('option', 'max', totalFrames);
		                frameNumberInput.val(1);
		                // 在成功获取视频总帧数后启用输入框
		                frameNumberInput.slider('enable');
		            } else {
		                console.error('无法获取视频总帧数');
		            }
		        },
		        error: function () {
		            console.error('无法从服务端获取视频总帧数');
		        }
		    });
		}
	    // 模拟根据视频和帧号获取缩略图的函数
	    function getThumbnailImage(videoFile, frameNumber) {
	        // 计算缩略图URL
	        const thumbnailURL = `/getThumbnail?frameNumber=${frameNumber}`;
	
	        // 更新视频播放器的当前时间
	        const currentTime = (frameNumber / totalFrames) * videoPlayer.duration;
	        videoPlayer.currentTime = currentTime;
	
	        return thumbnailURL;
	    }

	    // 当拖动条值发生变化时，获取并显示对应帧位置的缩略图
	    frameNumberInput.on('slidechange', function (event, ui) {
	        const frameNumber = ui.value;
	        const videoFile = videoFileInput[0].files[0];
	        if (videoFile) {
	            const thumbnailURL = getThumbnailImage(videoFile, frameNumber);
	            thumbnailImage.attr('src', thumbnailURL);
	        }
	    });
});