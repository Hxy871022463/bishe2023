<template>
    <div style="line-height: 60px; display: flex">
     <div style="flex: 1;">
       <span :class="collapseBtnClass" style="cursor: pointer; font-size: 18px" @click="collapse"></span>

         <el-breadcrumb separator="/" style="display: inline-block; margin-left: 10px">
             <el-breadcrumb-item :to="'/home'">首页</el-breadcrumb-item>
             <el-breadcrumb-item>{{ currentPathName }}</el-breadcrumb-item>
         </el-breadcrumb>

    </div>
    <el-dropdown style="width: 150px; cursor: pointer; text-align: right"  @command="handleCommand">
      <div style="display: inline-block">
       <img :src="user.avatarUrl" alt=""
            style="width: 30px; border-radius: 50%; position: relative; top: 10px; right: 5px">
         <span>{{ user.nickname }}</span><i class="el-icon-arrow-down" style="margin-left: 5px"></i>
      </div>
        <el-dropdown-menu slot="dropdown" style="width: 100px; text-align: center">
            <el-dropdown-item style="font-size: 14px; padding: 5px 0" command="repass">
                <span>修改密码</span>
            </el-dropdown-item>
            <el-dropdown-item style="font-size: 14px; padding: 5px 0" command="profile">
                <span>个人信息</span>
            </el-dropdown-item>
            <el-dropdown-item style="font-size: 14px; padding: 5px 0" command="exit">
                <span>退出</span>
            </el-dropdown-item>
        </el-dropdown-menu>
    </el-dropdown>
 </div>
</template>

<script>
import {resetRouter} from "@/router";

export default {
  name: "Header",
  props: {
    collapseBtnClass: String,
    user: Object
  },
  computed:{
    currentPathName () {
      return this.$store.state.currentPathName; //需要监听的数据
    }
  },
  data(){
    return {

    }
  },
  methods: {
    collapse() {
        // this.$parent.$parent.$parent.$parent.collapse()  // 通过4个 $parent 找到父组件，从而调用其折叠方法
        this.$emit("asideCollapse")
    },
    logout() {
        localStorage.removeItem("user")
        localStorage.removeItem("menus")
        this.$message.success("退出成功")
        this.$router.push("/login")

        // 重置路由
        resetRouter()
    },
      handleCommand(command){
          switch(command){
              case 'repass': this.$router.push('/password')
                  break
              case 'profile' : this.$router.push('/person')
                  break
              case 'exit' : this.logout()
                  break
          }
      }
  }
}
</script>

<style scoped>

</style>