<template>
  <AppLayout>
    <template #nav>
      <NavSidebar :active-tab="activeTab" @select-tab="onTabChange" />
    </template>

    <template #conversations>
      <ConversationPanel>
        <ConversationSearch v-model="searchText" />
        <div class="conv-list">
          <template v-if="activeTab === 'chat'">
            <ConversationItem
              v-for="friend in filteredFriends"
              :key="'f-' + friend.friendId"
              :avatar="friend.avatarUrl"
              :name="friend.remark || friend.nickname"
              :preview="friend.signature || ''"
              :time-text="''"
              :unread="0"
              :status="friend.status"
              :active="Number(friend.friendId) === chatStore.currentPeerId"
              @click="selectChat(friend.friendId)"
            />
            <ConversationItem
              v-for="g in filteredGroups"
              :key="'g-' + g.id"
              :avatar="g.avatarUrl"
              :name="g.name"
              :preview="`${g.memberCount} 人`"
              :time-text="''"
              :unread="0"
              :active="chatStore.currentGroupId === Number(g.id)"
              @click="selectGroupChat(Number(g.id))"
            />
            <LcEmpty v-if="filteredFriends.length === 0 && filteredGroups.length === 0" description="暂无会话" />
          </template>

          <template v-if="activeTab === 'friend'">
            <div class="conv-action-bar">
              <LcButton variant="primary" size="sm" @click="showAddFriend = true">添加好友</LcButton>
            </div>
            <div v-if="filteredRequests.length > 0" class="friend-requests">
              <div class="friend-requests__title">好友请求 ({{ filteredRequests.length }})</div>
              <div v-for="req in filteredRequests" :key="req.friendId" class="friend-requests__item">
                <LcAvatar :src="req.avatarUrl" :name="req.nickname" size="sm" />
                <div class="friend-requests__info">
                  <span class="friend-requests__name">{{ req.remark || req.nickname }}</span>
                  <span class="friend-requests__code">ID: {{ req.userCode }}</span>
                </div>
                <LcButton variant="primary" size="sm" :loading="acceptingIds.has(req.friendId)" @click="handleAcceptFriend(req.friendId)">接受</LcButton>
                <LcButton variant="secondary" size="sm" :loading="rejectingIds.has(req.friendId)" @click="handleRejectFriend(req.friendId)">拒绝</LcButton>
              </div>
            </div>
            <ConversationItem
              v-for="friend in filteredFriends"
              :key="friend.friendId"
              :avatar="friend.avatarUrl"
              :name="friend.remark || friend.nickname"
              :preview="friend.signature || ''"
              :time-text="''"
              :unread="0"
              :status="friend.status"
              :active="Number(friend.friendId) === chatStore.currentPeerId"
              @click="selectChat(friend.friendId)"
              @avatar-click="openFriendProfile(friend)"
            />
            <LcEmpty v-if="filteredFriends.length === 0" description="暂无好友" />
          </template>

          <template v-if="activeTab === 'group'">
            <div class="conv-action-bar">
              <LcButton variant="primary" size="sm" @click="showCreateGroup = true">创建群聊</LcButton>
            </div>
            <ConversationItem
              v-for="g in filteredGroups"
              :key="g.id"
              :avatar="g.avatarUrl"
              :name="g.name"
              :preview="`${g.memberCount} 人`"
              :time-text="''"
              :unread="0"
              :active="chatStore.currentGroupId === Number(g.id)"
              @click="selectGroupChat(Number(g.id))"
            />
            <LcEmpty v-if="filteredGroups.length === 0" description="暂无群聊" />
          </template>
        </div>
      </ConversationPanel>
    </template>

    <template #chat>
      <ChatPanel>
        <ChatWindow
          v-if="chatStore.currentPeerId || chatStore.currentGroupId"
          :key="chatStore.currentPeerId ?? chatStore.currentGroupId ?? 'welcome'"
          @clear="chatStore.reset()"
          @manage-group="openGroupManage"
        />
        <div v-else class="welcome">
          <div class="welcome-content">
            <svg width="56" height="56" viewBox="0 0 64 64" fill="none">
              <rect width="64" height="64" rx="16" fill="#10B981"/>
              <text x="50%" y="54%" dominant-baseline="middle" text-anchor="middle" fill="#FFFFFF" font-size="28" font-weight="700" font-family="Inter, sans-serif">LC</text>
            </svg>
            <h2>LinkChat</h2>
            <p>选择一个会话开始聊天</p>
            <LcButton variant="primary" @click="showAddFriend = true">添加好友</LcButton>
          </div>
        </div>
      </ChatPanel>
    </template>
  </AppLayout>

  <!-- 添加好友弹窗 -->
  <LcModal v-if="showAddFriend" :model-value="true" title="添加好友" width="400px" @update:model-value="handleCloseAddFriend">
    <div class="add-friend">
      <div class="add-friend__search">
        <input v-model="addKeyword" placeholder="通过邮箱或用户码搜索" class="native-input" @keydown.enter="handleSearchFriend" />
        <LcButton variant="primary" size="sm" :disabled="!addKeyword.trim()" :loading="searchingFriend" @click="handleSearchFriend">搜索</LcButton>
      </div>
      <div v-if="searchResults.length > 0" class="add-friend__results">
        <div class="add-friend__result-title">搜索结果 ({{ searchResults.length }})</div>
        <div v-for="user in searchResults" :key="user.id" class="add-friend__result-item">
          <LcAvatar :src="user.avatarUrl" :name="user.nickname" size="sm" />
          <div class="add-friend__result-info">
            <span class="add-friend__result-name">{{ user.nickname }}</span>
            <span class="add-friend__result-code">ID: {{ user.userCode }}</span>
          </div>
          <LcButton variant="primary" size="sm" :loading="addingIds.has(Number(user.id))" @click="handleAddFriendByUser(user)">添加</LcButton>
        </div>
      </div>
      <LcEmpty v-else-if="searched" description="未找到用户" />
    </div>
    <template #footer>
      <LcButton variant="secondary" @click="handleCloseAddFriend">关闭</LcButton>
    </template>
  </LcModal>

  <!-- 好友资料弹窗 -->
  <FriendProfileModal
    v-if="showFriendProfile && selectedFriend"
    :visible="true"
    :friend="selectedFriend"
    @close="showFriendProfile = false"
    @chat="(friendId: number) => { showFriendProfile = false; selectChat(friendId) }"
  />

  <!-- 创建群聊弹窗 -->
  <LcModal v-if="showCreateGroup" :model-value="true" title="创建群聊" width="420px" @update:model-value="showCreateGroup = false">
    <div class="form-group">
      <label class="form-label">群名称</label>
      <input v-model="newGroupName" placeholder="输入群名称" maxlength="30" class="native-input" @keydown.enter="handleCreateGroup" />
    </div>
    <div class="form-group" style="margin-top:12px">
      <label class="form-label">选择好友</label>
      <div class="member-select">
        <label v-for="f in friendStore.friends" :key="f.friendId" class="member-select__item">
          <input type="checkbox" :value="f.friendId" v-model="selectedMemberIds" />
          <span>{{ f.remark || f.nickname }}</span>
        </label>
        <p v-if="friendStore.friends.length === 0" class="member-select__empty">暂无好友可选</p>
      </div>
    </div>
    <template #footer>
      <LcButton variant="secondary" @click="showCreateGroup = false">取消</LcButton>
      <LcButton variant="primary" :disabled="!newGroupName.trim() || selectedMemberIds.length === 0" :loading="creatingGroup" @click="handleCreateGroup">创建</LcButton>
    </template>
  </LcModal>

  <!-- 群管理弹窗 -->
  <LcModal v-if="showGroupManage && managedGroup" :model-value="true" :title="managedGroup.name" width="420px" @update:model-value="showGroupManage = false">
    <div class="group-manage">
      <!-- 群设置（群主/管理员） -->
      <div v-if="myRoleInGroup <= 1" class="group-manage__section">
        <div class="group-manage__section-title">群设置</div>
        <div class="group-manage__row">
          <span class="group-manage__label">群名称</span>
          <div class="group-manage__input-row">
            <input v-model="editingGroupName" :placeholder="managedGroup.name" maxlength="30" class="native-input native-input--sm" />
            <LcButton variant="primary" size="sm" :loading="savingGroupName" @click="handleSaveGroupName">保存</LcButton>
          </div>
        </div>
        <div class="group-manage__row">
          <span class="group-manage__label">群公告</span>
          <div class="group-manage__input-row">
            <input v-model="editingAnnouncement" :placeholder="managedGroup.announcement || '暂无公告'" maxlength="200" class="native-input native-input--sm" />
            <LcButton variant="primary" size="sm" :loading="savingAnnouncement" @click="handleSaveAnnouncement">保存</LcButton>
          </div>
        </div>
        <!-- 群主专属 -->
        <template v-if="myRoleInGroup === 0">
          <div class="group-manage__row">
            <span class="group-manage__label">全员禁言</span>
            <LcButton :variant="managedGroup.isMuted ? 'secondary' : 'primary'" size="sm" :loading="togglingMuteAll" @click="handleToggleMuteAll">
              {{ managedGroup.isMuted ? '取消全员禁言' : '全员禁言' }}
            </LcButton>
          </div>
          <div class="group-manage__row">
            <span class="group-manage__label">转让群主</span>
            <select v-model="transferTargetId" class="native-input native-input--sm" style="width:160px">
              <option value="">-- 请选择 --</option>
              <option v-for="m in managedMembers" :key="m.id" :value="m.id" :disabled="String(m.id) === String(authStore.userInfo?.id)">
                {{ m.nickname }} {{ (m as any).role === 1 ? '(管理员)' : '' }}
              </option>
            </select>
            <LcButton variant="primary" size="sm" :disabled="!transferTargetId" :loading="transferring" @click="handleTransferOwnership">转让</LcButton>
          </div>
          <div class="group-manage__row group-manage__row--danger">
            <span class="group-manage__label">解散群聊</span>
            <LcButton variant="secondary" size="sm" class="btn-danger" :loading="dismissing" @click="handleDismissGroup">解散</LcButton>
          </div>
        </template>
      </div>

      <!-- 退出群聊（非群主） -->
      <div v-if="myRoleInGroup !== -1 && myRoleInGroup !== 0" class="group-manage__section">
        <div class="group-manage__row group-manage__row--danger">
          <span class="group-manage__label">退出群聊</span>
          <LcButton variant="secondary" size="sm" class="btn-danger" :loading="leaving" @click="handleLeaveGroup">退出</LcButton>
        </div>
      </div>

      <!-- 邀请好友（群主/管理员） -->
      <div v-if="myRoleInGroup <= 1" class="group-manage__section">
        <div class="group-manage__section-title">邀请好友</div>
        <div class="member-select">
          <label v-for="f in friendStore.friends" :key="f.friendId" class="member-select__item">
            <input type="checkbox" :value="f.friendId" v-model="inviteMemberIds" />
            <span>{{ f.remark || f.nickname }}</span>
          </label>
          <p v-if="friendStore.friends.length === 0" class="member-select__empty">暂无好友可选</p>
        </div>
        <LcButton variant="primary" size="sm" :disabled="inviteMemberIds.length === 0" :loading="inviting" @click="handleInviteMembers" style="margin-top:8px">邀请</LcButton>
      </div>

      <!-- 成员列表 + 点击操作 -->
      <div class="group-manage__section">
        <div class="group-manage__section-title">群成员 ({{ managedGroup.memberCount }})</div>
        <div class="member-list">
          <div v-for="m in managedMembers" :key="m.id" class="member-list__item" :class="{ 'member-list__item--active': selectedMemberId === m.id }" @click="onClickMember(m)">
            <LcAvatar :src="m.avatarUrl" :name="m.nickname" size="sm" />
            <div class="member-list__info">
              <span class="member-list__name">
                {{ m.nickname }}
                <span v-if="getMemberRoleLabel(m) === '群主'" class="member-tag member-tag--owner">群主</span>
                <span v-else-if="getMemberRoleLabel(m) === '管理员'" class="member-tag member-tag--admin">管理员</span>
              </span>
              <span v-if="m.isMuted" class="member-list__muted">已禁言</span>
            </div>
          </div>
          <LcEmpty v-if="managedMembers.length === 0" description="暂无成员" />
        </div>

        <!-- 选中成员操作 -->
        <div v-if="selectedMemberId && selectedMemberId !== String(authStore.userInfo?.id)" class="member-actions">
          <div class="member-actions__title">对 "{{ selectedMemberName }}" 操作</div>
          <div class="member-actions__btns">
            <LcButton v-if="canKickMember" variant="secondary" size="sm" class="btn-danger" :loading="kicking" @click="handleKickMember">踢出</LcButton>
            <LcButton v-if="canToggleAdmin" variant="secondary" size="sm" :loading="togglingAdmin" @click="handleToggleAdmin">
              {{ selectedMemberIsAdmin ? '撤销管理员' : '设为管理员' }}
            </LcButton>
            <LcButton v-if="canMuteMember" variant="secondary" size="sm" :loading="togglingMemberMute" @click="handleToggleMemberMute">
              {{ selectedMemberIsMuted ? '取消禁言' : '禁言' }}
            </LcButton>
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <LcButton variant="secondary" @click="showGroupManage = false">关闭</LcButton>
    </template>
  </LcModal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useFriendStore } from '@/stores/friend'
import { useGroupStore } from '@/stores/group'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'
import { toast } from '@/components/design/LcToast.ts'
import AppLayout from '@/components/layout/AppLayout.vue'
import NavSidebar from '@/components/layout/NavSidebar.vue'
import ConversationPanel from '@/components/layout/ConversationPanel.vue'
import ChatPanel from '@/components/layout/ChatPanel.vue'
import ChatWindow from '@/components/chat/ChatWindow.vue'
import ConversationItem from '@/components/conversation/ConversationItem.vue'
import ConversationSearch from '@/components/conversation/ConversationSearch.vue'
import LcAvatar from '@/components/design/LcAvatar.vue'
import LcButton from '@/components/design/LcButton.vue'
import LcModal from '@/components/design/LcModal.vue'
import LcEmpty from '@/components/design/LcEmpty.vue'
import FriendProfileModal from '@/components/friend/FriendProfileModal.vue'
import { userApi } from '@/api/user'
import { groupApi } from '@/api/group'
import type { Friend } from '@/types/friend'
import type { User } from '@/types/user'
import type { Group } from '@/types/group'

const chatStore = useChatStore()
const friendStore = useFriendStore()
const groupStore = useGroupStore()
const authStore = useAuthStore()
useWebSocket()

const activeTab = ref('chat')
const searchText = ref('')
const showAddFriend = ref(false)
const addKeyword = ref('')
const showFriendProfile = ref(false)
const selectedFriend = ref<Friend | null>(null)
const showCreateGroup = ref(false)
const newGroupName = ref('')
const selectedMemberIds = ref<number[]>([])
const creatingGroup = ref(false)
const acceptingIds = ref(new Set<number>())
const rejectingIds = ref(new Set<number>())
const searchingFriend = ref(false)
const searched = ref(false)
const searchResults = ref<User[]>([])
const addingIds = ref(new Set<number>())

// 群管理
const showGroupManage = ref(false)
const managedGroup = ref<Group | null>(null)
const managedMembers = ref<User[]>([])
const selectedMemberId = ref<string | null>(null)
const editingGroupName = ref('')
const editingAnnouncement = ref('')
const inviteMemberIds = ref<number[]>([])
const transferTargetId = ref('')
const savingGroupName = ref(false)
const savingAnnouncement = ref(false)
const togglingMuteAll = ref(false)
const leaving = ref(false)
const dismissing = ref(false)
const inviting = ref(false)
const transferring = ref(false)
const kicking = ref(false)
const togglingAdmin = ref(false)
const togglingMemberMute = ref(false)

function matchText(text: string | undefined | null, kw: string): boolean {
  return (text || '').toLowerCase().includes(kw)
}

const filteredFriends = computed(() => {
  const list = friendStore.friends
  if (!searchText.value) return list
  const kw = searchText.value.toLowerCase()
  return list.filter(f =>
    matchText(f.remark || f.nickname, kw) ||
    matchText(f.signature, kw) ||
    matchText(f.userCode, kw)
  )
})

const filteredGroups = computed(() => {
  const list = groupStore.groups
  if (!searchText.value) return list
  const kw = searchText.value.toLowerCase()
  return list.filter(g => matchText(g.name, kw))
})

const filteredRequests = computed(() => {
  const list = friendStore.pendingRequests
  if (!searchText.value) return list
  const kw = searchText.value.toLowerCase()
  return list.filter(r =>
    matchText(r.remark || r.nickname, kw) ||
    matchText(r.userCode, kw)
  )
})

const myRoleInGroup = computed(() => {
  if (!managedGroup.value) return -1
  return managedGroup.value.myRole
})

const selectedMemberName = computed(() => {
  const m = managedMembers.value.find(m => m.id === selectedMemberId.value)
  return m?.nickname || ''
})

const selectedMember = computed(() => {
  return managedMembers.value.find(m => m.id === selectedMemberId.value)
})

const selectedMemberRole = computed(() => {
  return (selectedMember.value as any)?.role ?? -1
})

const selectedMemberIsAdmin = computed(() => selectedMemberRole.value === 1)
const selectedMemberIsOwner = computed(() => selectedMemberRole.value === 0)

const selectedMemberIsMuted = computed(() => {
  return (selectedMember.value?.isMuted ?? 0) === 1
})

// ROLE: 0=群主, 1=管理员, 2=成员
const canKickMember = computed(() => {
  if (!selectedMember.value) return false
  if (selectedMemberIsOwner.value) return false
  if (myRoleInGroup.value === 0) return true
  if (myRoleInGroup.value === 1 && !selectedMemberIsAdmin.value) return true
  return false
})

const canToggleAdmin = computed(() => {
  if (myRoleInGroup.value !== 0) return false
  if (selectedMemberIsOwner.value) return false
  return true
})

const canMuteMember = computed(() => {
  if (!selectedMember.value) return false
  if (selectedMemberIsOwner.value) return false
  if (myRoleInGroup.value === 0) return true
  if (myRoleInGroup.value === 1 && !selectedMemberIsAdmin.value) return true
  return false
})

function getMemberRoleLabel(m: any) {
  const role = (m as any).role ?? m.role
  if (role === 0) return '群主'
  if (role === 1) return '管理员'
  return '成员'
}

onMounted(async () => {
  await Promise.all([
    friendStore.loadFriends(),
    friendStore.loadPendingRequests(),
    groupStore.loadGroups()
  ])
})

function onTabChange(tab: string) {
  activeTab.value = tab
  searchText.value = ''
}

function openFriendProfile(friend: Friend) {
  selectedFriend.value = friend
  showFriendProfile.value = true
}

function selectChat(friendId: number) {
  chatStore.loadMessages(friendId)
}

function selectGroupChat(groupId: number) {
  chatStore.loadMessages(undefined, groupId)
}

function handleCloseAddFriend() {
  showAddFriend.value = false
  addKeyword.value = ''
  searchResults.value = []
  searched.value = false
}

async function handleSearchFriend() {
  if (!addKeyword.value.trim()) return
  searchingFriend.value = true
  searched.value = false
  try {
    const res = await userApi.searchUsers(addKeyword.value.trim())
    searchResults.value = res.data || []
    searched.value = true
  } catch (e: any) {
    toast.error(e.message || '搜索失败')
  } finally {
    searchingFriend.value = false
  }
}

async function handleAddFriendByUser(user: User) {
  try {
    addingIds.value.add(Number(user.id))
    await friendStore.addFriend(user.userCode || String(user.id))
    toast.success('好友请求已发送')
  } catch (e: any) {
    toast.error(e.message || '添加失败')
  } finally {
    addingIds.value.delete(Number(user.id))
  }
}

async function handleAcceptFriend(friendId: number) {
  acceptingIds.value.add(friendId)
  try {
    await friendStore.acceptFriend(friendId)
    toast.success('已接受好友请求')
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    acceptingIds.value.delete(friendId)
  }
}

async function handleRejectFriend(friendId: number) {
  rejectingIds.value.add(friendId)
  try {
    await friendStore.rejectFriend(friendId)
    toast.success('已拒绝好友请求')
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    rejectingIds.value.delete(friendId)
  }
}

async function handleCreateGroup() {
  if (!newGroupName.value.trim() || selectedMemberIds.value.length === 0) return
  creatingGroup.value = true
  try {
    await groupStore.createGroup(newGroupName.value.trim(), selectedMemberIds.value)
    toast.success('群聊创建成功')
    showCreateGroup.value = false
    newGroupName.value = ''
    selectedMemberIds.value = []
  } catch (e: any) {
    toast.error(e.message || '创建失败')
  } finally {
    creatingGroup.value = false
  }
}

// ============= 群管理 =============

function onClickMember(m: User) {
  selectedMemberId.value = selectedMemberId.value === m.id ? null : m.id
}

async function openGroupManage(groupId: string) {
  const g = groupStore.groups.find(g => String(g.id) === String(groupId))
  if (!g) return
  managedGroup.value = { ...g }
  editingGroupName.value = ''
  editingAnnouncement.value = ''
  inviteMemberIds.value = []
  transferTargetId.value = ''
  selectedMemberId.value = null
  showGroupManage.value = true
  try {
    const res = await groupApi.getGroupMembers(g.id)
    managedMembers.value = res.data || []
  } catch (e: any) {
    toast.error(e.message || '获取成员列表失败')
  }
}

async function handleSaveGroupName() {
  if (!editingGroupName.value.trim() || !managedGroup.value) return
  savingGroupName.value = true
  try {
    await groupApi.updateGroupName(managedGroup.value.id, editingGroupName.value.trim())
    toast.success('群名称已更新')
    managedGroup.value.name = editingGroupName.value.trim()
    editingGroupName.value = ''
    groupStore.loadGroups()
  } catch (e: any) {
    toast.error(e.message || '保存失败')
  } finally {
    savingGroupName.value = false
  }
}

async function handleSaveAnnouncement() {
  if (!managedGroup.value) return
  savingAnnouncement.value = true
  try {
    await groupApi.updateAnnouncement(managedGroup.value.id, editingAnnouncement.value)
    toast.success('群公告已更新')
    managedGroup.value.announcement = editingAnnouncement.value
    editingAnnouncement.value = ''
    groupStore.loadGroups()
  } catch (e: any) {
    toast.error(e.message || '保存失败')
  } finally {
    savingAnnouncement.value = false
  }
}

async function handleToggleMuteAll() {
  if (!managedGroup.value) return
  togglingMuteAll.value = true
  try {
    const newMuted = managedGroup.value.isMuted ? 0 : 1
    await groupApi.muteAll(managedGroup.value.id, newMuted === 1)
    toast.success(newMuted ? '已开启全员禁言' : '已关闭全员禁言')
    managedGroup.value.isMuted = newMuted
    groupStore.loadGroups()
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    togglingMuteAll.value = false
  }
}

async function handleTransferOwnership() {
  if (!transferTargetId.value || !managedGroup.value) return
  transferring.value = true
  try {
    await groupApi.transferOwnership(managedGroup.value.id, transferTargetId.value)
    toast.success('群主已转让')
    showGroupManage.value = false
    groupStore.loadGroups()
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    transferring.value = false
  }
}

async function handleDismissGroup() {
  if (!managedGroup.value) return
  if (!confirm('确定要解散该群聊吗？此操作不可撤销。')) return
  dismissing.value = true
  try {
    await groupApi.dismissGroup(managedGroup.value.id)
    toast.success('群聊已解散')
    showGroupManage.value = false
    chatStore.reset()
    groupStore.loadGroups()
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    dismissing.value = false
  }
}

async function handleLeaveGroup() {
  if (!managedGroup.value) return
  leaving.value = true
  try {
    await groupApi.leaveGroup(managedGroup.value.id)
    toast.success('已退出群聊')
    showGroupManage.value = false
    chatStore.reset()
    groupStore.loadGroups()
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    leaving.value = false
  }
}

async function handleInviteMembers() {
  if (!managedGroup.value || inviteMemberIds.value.length === 0) return
  inviting.value = true
  try {
    await groupApi.inviteMembers(managedGroup.value.id, inviteMemberIds.value)
    toast.success('已邀请')
    inviteMemberIds.value = []
    groupStore.loadGroups()
    const res = await groupApi.getGroupMembers(managedGroup.value.id)
    managedMembers.value = res.data || []
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    inviting.value = false
  }
}

async function handleKickMember() {
  if (!selectedMemberId.value || !managedGroup.value) return
  kicking.value = true
  try {
    await groupApi.removeMember(managedGroup.value.id, selectedMemberId.value)
    toast.success('已踢出')
    groupStore.loadGroups()
    selectedMemberId.value = null
    const res = await groupApi.getGroupMembers(managedGroup.value.id)
    managedMembers.value = res.data || []
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    kicking.value = false
  }
}

async function handleToggleAdmin() {
  if (!selectedMemberId.value || !managedGroup.value) return
  togglingAdmin.value = true
  try {
    if (selectedMemberIsAdmin.value) {
      await groupApi.removeAdmin(managedGroup.value.id, selectedMemberId.value)
      toast.success('已撤销管理员')
    } else {
      await groupApi.setAdmin(managedGroup.value.id, selectedMemberId.value)
      toast.success('已设为管理员')
    }
    groupStore.loadGroups()
    selectedMemberId.value = null
    const res = await groupApi.getGroupMembers(managedGroup.value.id)
    managedMembers.value = res.data || []
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    togglingAdmin.value = false
  }
}

async function handleToggleMemberMute() {
  if (!selectedMemberId.value || !managedGroup.value) return
  togglingMemberMute.value = true
  try {
    const newMuted = !selectedMemberIsMuted.value
    await groupApi.muteMember(managedGroup.value.id, selectedMemberId.value, newMuted)
    toast.success(newMuted ? '已禁言' : '已取消禁言')
    groupStore.loadGroups()
    selectedMemberId.value = null
    const res = await groupApi.getGroupMembers(managedGroup.value.id)
    managedMembers.value = res.data || []
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    togglingMemberMute.value = false
  }
}
</script>

<style scoped>
.native-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--lc-border);
  border-radius: var(--lc-radius-input);
  background: var(--lc-bg-input);
  font-family: var(--lc-font-family);
  font-size: var(--lc-font-size-lg);
  color: var(--lc-text-primary);
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  box-sizing: border-box;
}
.native-input:focus { border-color: var(--lc-border-focus); box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1); }
.native-input::placeholder { color: var(--lc-text-tertiary); }
.native-input--sm { padding: 6px 10px; font-size: var(--lc-font-size-sm); }

.conv-list { flex: 1; overflow-y: auto; padding: var(--lc-space-2) 0; }
.conv-action-bar { padding: var(--lc-space-2) var(--lc-space-4); border-bottom: 1px solid var(--lc-border-light); }

.friend-requests { padding: var(--lc-space-2) var(--lc-space-4); border-bottom: 1px solid var(--lc-border-light); }
.friend-requests__title { font-size: var(--lc-font-size-xs); color: var(--lc-text-tertiary); font-weight: var(--lc-font-weight-medium); margin-bottom: var(--lc-space-2); }
.friend-requests__item { display: flex; align-items: center; gap: var(--lc-space-2); padding: var(--lc-space-2) 0; }
.friend-requests__info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.friend-requests__name { font-size: var(--lc-font-size-sm); color: var(--lc-text-primary); font-weight: var(--lc-font-weight-medium); }
.friend-requests__code { font-size: var(--lc-font-size-xs); color: var(--lc-text-tertiary); }

.form-group { margin-bottom: var(--lc-space-3); }
.form-label { display: block; font-size: var(--lc-font-size-sm); color: var(--lc-text-secondary); margin-bottom: 4px; font-weight: var(--lc-font-weight-medium); }
.member-select { max-height: 200px; overflow-y: auto; border: 1px solid var(--lc-border); border-radius: var(--lc-radius-input); padding: var(--lc-space-2); }
.member-select__item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: var(--lc-font-size-sm); color: var(--lc-text-primary); cursor: pointer; }
.member-select__item input[type="checkbox"] { accent-color: var(--lc-primary); }
.member-select__empty { text-align: center; color: var(--lc-text-tertiary); font-size: var(--lc-font-size-sm); padding: var(--lc-space-4) 0; margin: 0; }

.add-friend__search { display: flex; gap: var(--lc-space-2); }
.add-friend__search .native-input { flex: 1; }
.add-friend__results { margin-top: var(--lc-space-4); max-height: 280px; overflow-y: auto; }
.add-friend__result-title { font-size: var(--lc-font-size-xs); color: var(--lc-text-tertiary); margin-bottom: var(--lc-space-2); }
.add-friend__result-item { display: flex; align-items: center; gap: var(--lc-space-3); padding: var(--lc-space-2) 0; }
.add-friend__result-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.add-friend__result-name { font-size: var(--lc-font-size-sm); color: var(--lc-text-primary); font-weight: var(--lc-font-weight-medium); }
.add-friend__result-code { font-size: var(--lc-font-size-xs); color: var(--lc-text-tertiary); }

.welcome { display: flex; align-items: center; justify-content: center; height: 100%; }
.welcome-content { text-align: center; }
.welcome-content h2 { font-size: var(--lc-font-size-3xl); color: var(--lc-text-primary); margin: var(--lc-space-4) 0 var(--lc-space-2); font-weight: var(--lc-font-weight-bold); }
.welcome-content p { color: var(--lc-text-secondary); margin: 0 0 var(--lc-space-6); font-size: var(--lc-font-size-base); }

/* 群管理 */
.group-manage { max-height: 60vh; overflow-y: auto; }
.group-manage__section { padding: var(--lc-space-3) 0; border-bottom: 1px solid var(--lc-border-light); }
.group-manage__section:last-child { border-bottom: none; }
.group-manage__section-title { font-size: var(--lc-font-size-sm); font-weight: var(--lc-font-weight-semibold); color: var(--lc-text-secondary); margin-bottom: var(--lc-space-2); }
.group-manage__row { display: flex; align-items: center; justify-content: space-between; padding: var(--lc-space-2) 0; gap: var(--lc-space-2); }
.group-manage__label { font-size: var(--lc-font-size-sm); color: var(--lc-text-primary); flex-shrink: 0; }
.group-manage__input-row { display: flex; gap: var(--lc-space-2); align-items: center; flex: 1; max-width: 260px; }
.group-manage__input-row .native-input--sm { flex: 1; }
.btn-danger { color: #e74c3c !important; border-color: #e74c3c !important; }

.member-list { max-height: 240px; overflow-y: auto; }
.member-list__item { display: flex; align-items: center; gap: var(--lc-space-3); padding: var(--lc-space-2); cursor: pointer; border-radius: var(--lc-radius-sm); }
.member-list__item:hover { background: var(--lc-bg-hover); }
.member-list__item--active { background: var(--lc-bg-hover); }
.member-list__info { display: flex; flex-direction: column; gap: 1px; flex: 1; }
.member-list__name { font-size: var(--lc-font-size-sm); color: var(--lc-text-primary); font-weight: var(--lc-font-weight-medium); display: flex; align-items: center; gap: 6px; }
.member-list__muted { font-size: var(--lc-font-size-xs); color: #e74c3c; }
.member-tag { font-size: 10px; padding: 1px 6px; border-radius: 3px; font-weight: var(--lc-font-weight-normal); }
.member-tag--owner { background: #ffeaa7; color: #d68910; }
.member-tag--admin { background: #dfe6e9; color: #636e72; }

.member-actions { margin-top: var(--lc-space-3); padding: var(--lc-space-3); background: var(--lc-bg-card); border-radius: var(--lc-radius-sm); }
.member-actions__title { font-size: var(--lc-font-size-sm); font-weight: var(--lc-font-weight-medium); color: var(--lc-text-primary); margin-bottom: var(--lc-space-2); }
.member-actions__btns { display: flex; gap: var(--lc-space-2); flex-wrap: wrap; }
</style>
