<template>
  <AppLayout>
    <template #nav>
      <NavSidebar active-tab="settings" @select-tab="onTabChange" />
    </template>
    <template #conversations>
      <ConversationPanel>
        <div class="settings-nav">
          <h3 class="settings-nav__title">设置</h3>
          <button class="settings-nav__back" @click="$router.push('/')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
            返回聊天
          </button>
        </div>
      </ConversationPanel>
    </template>
    <template #chat>
      <ChatPanel>
        <div class="settings-page">
          <div class="settings-body">
            <!-- 头像 -->
            <div class="section">
              <h3>头像</h3>
              <div class="avatar-section">
                <LcAvatar :src="avatarPreviewUrl || authStore.userInfo?.avatarUrl" :name="authStore.userInfo?.nickname || ''" size="lg" />
                <div class="avatar-section__actions">
                  <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="handleAvatarSelect" />
                  <LcButton variant="secondary" size="sm" @click="avatarInput?.click()">选择图片</LcButton>
                  <template v-if="avatarPreviewUrl">
                    <LcButton variant="primary" size="sm" :loading="uploadingAvatar" @click="handleAvatarUpload">上传</LcButton>
                    <LcButton variant="ghost" size="sm" @click="cancelAvatarPreview">取消</LcButton>
                  </template>
                </div>
              </div>
            </div>

            <!-- 基本信息 -->
            <div class="section">
              <h3>基本信息</h3>
              <div class="form-group">
                <label>邮箱</label>
                <LcInput :model-value="authStore.userInfo?.email || ''" disabled />
              </div>
              <div class="form-group">
                <label>用户码</label>
                <LcInput :model-value="authStore.userInfo?.userCode || ''" disabled />
              </div>
              <div class="form-group">
                <label>昵称</label>
                <input v-model="profile.nickname" maxlength="20" class="native-input" />
              </div>
              <div class="form-group">
                <label>签名</label>
                <input v-model="profile.signature" maxlength="100" class="native-input" />
              </div>
              <LcButton variant="primary" @click="handleSaveProfile">保存</LcButton>
            </div>

            <!-- 修改密码 -->
            <div class="section">
              <h3>修改密码</h3>
              <div class="form-group">
                <label>原密码</label>
                <input v-model="pwdForm.oldPwd" type="password" placeholder="输入原密码" class="native-input" />
              </div>
              <div class="form-group">
                <label>新密码</label>
                <input v-model="pwdForm.newPwd" type="password" placeholder="输入新密码（6-32位）" class="native-input" />
              </div>
              <LcButton variant="primary" @click="handleChangePwd">修改密码</LcButton>
            </div>

            <!-- 退出 -->
            <div class="section">
              <LcButton variant="danger" @click="authStore.logout()">退出登录</LcButton>
            </div>
          </div>
        </div>
      </ChatPanel>
    </template>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/user'
import { authApi } from '@/api/auth'
import { toast } from '@/components/design/LcToast.ts'
import AppLayout from '@/components/layout/AppLayout.vue'
import NavSidebar from '@/components/layout/NavSidebar.vue'
import ConversationPanel from '@/components/layout/ConversationPanel.vue'
import ChatPanel from '@/components/layout/ChatPanel.vue'
import LcAvatar from '@/components/design/LcAvatar.vue'
import LcButton from '@/components/design/LcButton.vue'
import LcInput from '@/components/design/LcInput.vue'

const router = useRouter()
const authStore = useAuthStore()
const avatarInput = ref<HTMLInputElement>()
const avatarPreviewUrl = ref('')
const selectedAvatarFile = ref<File | null>(null)
const uploadingAvatar = ref(false)

const profile = reactive({ nickname: '', signature: '' })
const pwdForm = reactive({ oldPwd: '', newPwd: '' })

onMounted(() => {
  profile.nickname = authStore.userInfo?.nickname || ''
  profile.signature = authStore.userInfo?.signature || ''
})

onUnmounted(() => {
  if (avatarPreviewUrl.value) URL.revokeObjectURL(avatarPreviewUrl.value)
})

function onTabChange(tab: string) {
  if (tab === 'chat') router.push('/')
}

function handleAvatarSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  // 预览
  if (avatarPreviewUrl.value) URL.revokeObjectURL(avatarPreviewUrl.value)
  selectedAvatarFile.value = file
  avatarPreviewUrl.value = URL.createObjectURL(file)
  target.value = ''
}

function cancelAvatarPreview() {
  if (avatarPreviewUrl.value) URL.revokeObjectURL(avatarPreviewUrl.value)
  avatarPreviewUrl.value = ''
  selectedAvatarFile.value = null
}

async function handleAvatarUpload() {
  if (!selectedAvatarFile.value) return
  uploadingAvatar.value = true
  try {
    await userApi.uploadAvatar(selectedAvatarFile.value)
    await authStore.fetchUserInfo()
    toast.success('头像更新成功')
    cancelAvatarPreview()
  } catch (e: any) {
    toast.error(e.message || '上传失败')
  } finally {
    uploadingAvatar.value = false
  }
}

async function handleSaveProfile() {
  try {
    await userApi.updateProfile({ nickname: profile.nickname, signature: profile.signature })
    await authStore.fetchUserInfo()
    toast.success('保存成功')
  } catch (e: any) {
    toast.error(e.message || '保存失败')
  }
}

async function handleChangePwd() {
  if (!pwdForm.oldPwd || !pwdForm.newPwd) { toast.warning('请填写完整'); return }
  if (pwdForm.newPwd.length < 6) { toast.warning('新密码至少6位'); return }
  try {
    await authApi.changePassword({ oldPassword: pwdForm.oldPwd, newPassword: pwdForm.newPwd })
    toast.success('密码修改成功，请重新登录')
    authStore.logout()
  } catch (e: any) {
    toast.error(e.message || '修改失败')
  }
}
</script>

<style scoped>
.settings-nav {
  padding: var(--lc-space-4);
  border-bottom: 1px solid var(--lc-border-light);
}

.settings-nav__title {
  font-size: var(--lc-font-size-lg);
  font-weight: var(--lc-font-weight-semibold);
  margin: 0 0 var(--lc-space-2);
}

.settings-nav__back {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--lc-font-size-sm);
  color: var(--lc-text-secondary);
  cursor: pointer;
  border: none;
  background: none;
  padding: 0;
}
.settings-nav__back:hover { color: var(--lc-primary); }

.settings-page { height: 100%; display: flex; flex-direction: column; }
.settings-body { flex: 1; overflow-y: auto; padding: var(--lc-space-6); max-width: 520px; }

.section { margin-bottom: var(--lc-space-8); }
.section h3 {
  font-size: var(--lc-font-size-md);
  font-weight: var(--lc-font-weight-semibold);
  margin: 0 0 var(--lc-space-4);
  padding-bottom: var(--lc-space-2);
  border-bottom: 1px solid var(--lc-border-light);
  color: var(--lc-text-primary);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: var(--lc-space-4);
}

.avatar-section__actions {
  display: flex;
  align-items: center;
  gap: var(--lc-space-2);
}

.native-input {
  padding: 10px 14px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-input);
  background: var(--lc-bg-input);
  font-family: var(--lc-font-family);
  font-size: var(--lc-font-size-lg);
  color: var(--lc-text-primary);
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  width: 100%;
  box-sizing: border-box;
}
.native-input:focus { border-color: var(--lc-border-focus); box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1); }
.native-input::placeholder { color: var(--lc-text-tertiary); }

.form-group {
  margin-bottom: var(--lc-space-3);
}
.form-group label {
  display: block;
  font-size: var(--lc-font-size-sm);
  color: var(--lc-text-secondary);
  margin-bottom: 4px;
  font-weight: var(--lc-font-weight-medium);
}
</style>
