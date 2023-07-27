<template>
  <div>
    <input type="file" ref="fileInput" @change="onFileChange" />
    <button @click="startUpload">开始上传</button>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import axios from 'axios';

export default defineComponent({
  data() {
    return {
      file: null as File | null,
      identifier: '',
      chunkSize: 1024 * 1024, // 1MB 分片大小
      resumePosition: 0, // 默认从头开始上传
    };
  },
  methods: {
    onFileChange(event: Event) {
      const target = event.target as HTMLInputElement;
      if (target.files && target.files.length > 0) {
        this.file = target.files[0];
      }
    },
    async startUpload() {
      if (!this.file) return;

      this.identifier = this.generateIdentifier(); // 生成上传标识符
      let totalChunks = Math.ceil(this.file.size / this.chunkSize);

      for (let chunkNumber = this.resumePosition; chunkNumber < totalChunks; chunkNumber++) {
        let startByte = chunkNumber * this.chunkSize;
        let endByte = Math.min(startByte + this.chunkSize, this.file.size);
        let chunk = this.file.slice(startByte, endByte);
        await this.uploadChunk(chunk, chunkNumber, totalChunks, this.identifier);
      }

      alert('文件上传完成');
    },
    async uploadChunk(chunk: Blob, chunkNumber: number, totalChunks: number, identifier: string) {
      let formData = new FormData();
      formData.append('file', new File([chunk], this.file?.name || '')); // 设置文件名为this.file.name
formData.append('chunkNumber', chunkNumber.toString()); // 将chunkNumber转换为字符串类型
      formData.append('totalChunks', totalChunks.toString()); // 将totalChunks转换为字符串类型
      formData.append('identifier', identifier);

      try {
        await axios.post('http://localhost:9527/sign/upload', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        });
      } catch (error) {
        console.error('分片上传失败', error);
        throw error;
      }
    },
    generateIdentifier() {
      // 生成唯一的上传标识符，可以使用uuid库来生成
      // 这里简化处理，直接使用时间戳作为标识符
      return Date.now().toString();
    },
    async getResumePosition() {
      try {
        let response = await axios.get('http://localhost:9527/sign/resume', {
          params: {
            identifier: this.identifier,
          },
        });
        this.resumePosition = response.data; // 更新继续位置
      } catch (error) {
        console.error('获取继续位置失败', error);
        throw error;
      }
    },
  },
  async mounted() {
    // 在组件加载时获取继续位置
    await this.getResumePosition();
  },
});
</script>
