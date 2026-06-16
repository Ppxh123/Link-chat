<template>
  <LcModal :model-value="visible" title="好友资料" width="400px" @update:model-value="$emit('close')">
    <div class="friend-profile">
      <div class="friend-profile__header">
        <LcAvatar :src="friend.avatarUrl" :name="friend.nickname" size="lg" :status="statusLabel" />
        <h3 class="friend-profile__name">{{ friend.remark || friend.nickname }}</h3>
        <p class="friend-profile__code">ID: {{ friend.userCode }}</p>
      </div>

      <div class="friend-profile__info">
        <div class="friend-profile__row">
          <span class="friend-profile__label">昵称</span>
          <span class="friend-profile__value">{{ friend.nickname }}</span>
        </div>
        <div class="friend-profile__row">
          <span class="friend-profile__label">备注</span>
          <span class="friend-profile__value">{{ friend.remark || '未设置' }}</span>
        </div>
        <div class="friend-profile__row">
          <span class="friend-profile__label">签名</span>
          <span class="friend-profile__value">{{ friend.signature || '未设置' }}</span>
        </div>
        <div class="friend-profile__row">
          <span class="friend-profile__label">状态</span>
          <span class="friend-profile__value" :class="statusClass">{{ statusText }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <LcButton variant="danger" :loading="deleting" @click="showDeleteConfirm = true">删除好友</LcButton>
      <LcButton variant="secondary" @click="$emit('close')">关闭</LcButton>
      <LcButton variant="primary" @click="$emit('chat', friend.friendId)">发消息</LcButton>
    </template>
  </LcModal>

  <!-- 删除确认弹窗 -->
  <LcModal v-if="showDeleteConfirm" :model-value="true" title="删除好友" width="360px" @update:model-value="showDeleteConfirm = false">
    <p style="text-align:center; color:var(--lc-text-secondary); margin:0;">
      确定要删除好友「{{ friend.remark || friend.nickname }}」吗？<br/>删除后聊天记录将保留。
    </p>
    <template #footer>
      <LcButton variant="secondary" @click="showDeleteConfirm = false">取消</LcButton>
      <LcButton variant="danger" :loading="deleting" @click="handleDelete">确定删除</LcButton>
    </template>
  </LcModal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import LcModal from '@/components/design/LcModal.vue'
import LcAvatar from '@/components/design/LcAvatar.vue'
import LcButton from '@/components/design/LcButton.vue'
import { useFriendStore } from '@/stores/friend'
import { toast } from '@/components/design/LcToast.ts'
import type { Friend } from '@/types/friend'

const props = defineProps<{
  visible: boolean
  friend: Friend
}>()

const emit = defineEmits<{
  close: []
  chat: [friendId: number]
}>()

const friendStore = useFriendStore()
const deleting = ref(false)
const showDeleteConfirm = ref(false)

const statusLabel = computed(() => {
  if (props.friend.status === 1) return 'online'
  if (props.friend.status === 2) return 'busy'
  return 'offline'
})

const statusText = computed(() => {
  if (props.friend.status === 1) return '在线'
  if (props.friend.status === 2) return '忙碌'
  return '离线'
})

const statusClass = computed(() => {
  if (props.friend.status === 1) return 'friend-profile__status--online'
  if (props.friend.status === 2) return 'friend-profile__status--busy'
  return 'friend-profile__status--offline'
})

async function handleDelete() {
  showDeleteConfirm.value = false
  deleting.value = true
  try {
    await friendStore.deleteFriend(props.friend.friendId)
    toast.success('好友已删除')
    emit('close')
  } catch (e: any) {
    toast.error(e?.response?.data?.message || '删除失败')
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.friend-profile { padding: var(--lc-space-2) 0; }
.friend-profile__header {
  text-align: center;
  padding-bottom: var(--lc-space-6);
  border-bottom: 1px solid var(--lc-border-light);
  margin-bottom: var(--lc-space-4);
}
.friend-profile__name {
  font-size: var(--lc-font-size-xl);
  font-weight: var(--lc-font-weight-semibold);
  color: var(--lc-text-primary);
  margin: var(--lc-space-3) 0 4px;
}
.friend-profile__code {
  font-size: var(--lc-font-size-sm);
  color: var(--lc-text-tertiary);
  margin: 0;
}
.friend-profile__info {
  display: flex;
  flex-direction: column;
  gap: var(--lc-space-3);
}
.friend-profile__row {
  display: flex;
  align-items: center;
  gap: var(--lc-space-3);
}
.friend-profile__label {
  width: 56px;
  flex-shrink: 0;
  font-size: var(--lc-font-size-sm);
  color: var(--lc-text-tertiary);
}
.friend-profile__value {
  font-size: var(--lc-font-size-sm);
  color: var(--lc-text-primary);
}
.friend-profile__status--online { color: var(--lc-success); }
.friend-profile__status--busy { color: var(--lc-warning); }
.friend-profile__status--offline { color: var(--lc-text-tertiary); }
</style>
