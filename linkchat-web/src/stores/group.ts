import { defineStore } from 'pinia'
import { ref } from 'vue'
import { groupApi } from '@/api/group'
import type { Group } from '@/types/group'

export const useGroupStore = defineStore('group', () => {
  const groups = ref<Group[]>([])

  async function loadGroups() {
    const res = await groupApi.getUserGroups()
    groups.value = res.data
  }

  async function createGroup(name: string, memberIds: number[]) {
    const res = await groupApi.createGroup({ name, memberIds })
    groups.value.unshift(res.data)
    return res.data
  }

  return { groups, loadGroups, createGroup }
})