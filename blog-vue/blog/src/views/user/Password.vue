<template>
  <div>
    <div class="banner" :style="cover">
      <h1 class="banner-title">修改密码</h1>
    </div>
    <v-card class="blog-container">
      <div>
        <span class="info-title">密码</span>
      </div>
      <v-row class="info-wrapper">
        <v-col md="3" cols="12">
          <button id="pick-avatar">
            <v-avatar size="140">
              <img :src="this.$store.state.avatar" alt="Avatar" />
            </v-avatar>
          </button>
          <avatar-cropper
            @uploaded="uploadAvatar"
            trigger="#pick-avatar"
            upload-url="/api/users/avatar"
          />
        </v-col>
        <v-col md="7" cols="12">
          <v-text-field
            v-model="passwordForm.oldPassword"
            label="旧密码"
            placeholder="请输入旧密码"
            type="password"
          />
          <v-text-field
            v-model="passwordForm.newPassword"
            label="新密码"
            placeholder="请输入新密码"
            type="password"
          />
          <v-text-field
            v-model="passwordForm.confirmPassword"
            label="确认密码"
            placeholder="请再次输入新密码"
            type="password"
          />
          <v-btn @click="updataUserPassword" outlined class="mt-5">修改</v-btn>
        </v-col>
      </v-row>
    </v-card>
  </div>
</template>

<script>
import AvatarCropper from "vue-avatar-cropper";
export default {
  components: { AvatarCropper },
  data: function() {
    return {
      passwordForm: {
        oldPassword: "",
        newPassword: "",
        confirmPassword: ""
      }
    };
  },
  mounted() {
    console.log(this.$store.state.userId);
  },
  methods: {
    updataUserPassword() {
      if (this.passwordForm.newPassword.trim() === "") {
        this.$toast({ type: "error", message: "密码不能为空" });
        return false;
      }
      if (this.passwordForm.newPassword.length < 6) {
        this.$toast({ type: "error", message: "密码长度不能小于6位" });
        return false;
      }
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.$toast({ type: "error", message: "两次密码不一致" });
        return false;
      }
      this.axios
        .put(
          "/api/users/password?userId=" + this.$store.state.userId,
          this.passwordForm
        )
        .then(({ data }) => {
          if (data.flag) {
            this.passwordForm.newPassword = "";
            this.passwordForm.confirmPassword = "";
            this.$toast({ type: "success", message: "修改成功" });
          } else {
            this.$toast({ type: "error", message: data.message });
          }
        });
    },
    uploadAvatar(data) {
      if (data.flag) {
        this.$store.commit("updateAvatar", data.data);
        this.$toast({ type: "success", message: "上传成功" });
      } else {
        this.$toast({ type: "error", message: data.message });
      }
    },
    openEmailModel() {
      this.$store.state.emailFlag = true;
    }
  },
  computed: {
    email() {
      return this.$store.state.email;
    },
    loginType() {
      return this.$store.state.loginType;
    },
    cover() {
      var cover = "";
      this.$store.state.blogInfo.pageList.forEach(item => {
        if (item.pageLabel === "password") {
          cover = item.pageCover;
        }
      });
      return "background: url(" + cover + ") center center / cover no-repeat";
    }
  }
};
</script>

<style scoped>
.info-title {
  font-size: 1.25rem;
  font-weight: bold;
}
.info-wrapper {
  margin-top: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
#pick-avatar {
  outline: none;
}
</style>
