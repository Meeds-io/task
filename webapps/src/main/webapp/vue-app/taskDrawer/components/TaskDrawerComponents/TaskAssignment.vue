<!--
  This file is part of the Meeds project (https://meeds.io/).
  Copyright (C) 2022 Meeds Association
  contact@meeds.io
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
  You should have received a copy of the GNU Lesser General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <div class="assigneeParent">
    <v-menu
      id="assigneeMenu"
      v-model="globalMenu"
      :content-class="menuId"
      :close-on-content-click="false"
      :nudge-left="0"
      :max-width="300"
      attach
      transition="scale-transition"
      class="taskAssignMenu"
      offset-y
      allow-overflow
      eager
      bottom>
      <template #activator="{ on }">
        <div class="d-flex align-center taskAssignItem" v-on="on">
          <div 
            v-if="taskAssigneeObj && taskAssigneeObj.profile && taskAssigneeObj.profile.fullName" 
            class="assigneeName overflow-hidden">
            <exo-user-avatar
              :profile-id="taskAssigneeObj.remoteId"
              :url="null"
              :size="24"
              extra-class="pe-2 overflow-hidden"
              popover />
          </div>
          <span
            v-if="taskCoworkers.length > 0"
            class="user-name coworkerNumber pe-2 caption font-italic lighten-2"> +{{ taskCoworkers.length }} {{ $t('label.coworker') }}
          </span>
          <a class="taskAssignBtn mt-n1">
            <v-icon size="15" class="my-auto me-1 icon-default-color">fa fa-user-plus</v-icon>
            <span class="text-decoration-underline">{{ $t('label.assign') }}</span>
          </a>
        </div>
      </template>
      <v-card class="pb-4">
        <v-card-text class="assignTaskMenu pb-0 d-flex justify-space-between">
          <span>{{ $t('label.assignTo') }} :</span>
          <a class="ms-4" @click="assignToMe()">
            <v-icon size="16" class="primary--text me-1">fa fa-user</v-icon>
            <span>{{ $t('label.addMe') }}</span>
          </a>
        </v-card-text>
        <exo-identity-suggester
          ref="autoFocusInput3"
          :labels="suggesterLabels"
          :value="taskAssigneeObj"
          :search-options="searchOptions"
          name="taskAssignee"
          type-of-relations="''"
          height="40"
          include-users
          @input="assigneeValueChanged($event)"
          @removeValue="removeAssignee()" />
        <v-divider class="mt-4 mb-4" />
        <v-card-text class="assignTaskMenu pt-0 pb-0 d-flex justify-space-between">
          <span>{{ $t('label.coworkers') }} :</span>
          <a class="ms-4" @click="setMeAsCoworker()">
            <v-icon size="16" class="primary--text me-1">fa fa-user-friends</v-icon>
            <span>{{ $t('label.addMe') }}</span>
          </a>
        </v-card-text>
        <exo-identity-suggester
          ref="autoFocusInput3"
          :labels="suggesterLabels"
          :value="taskCoworkerObj"
          :search-options="searchOptions"
          name="taskCoworkers"
          type-of-relations="''"
          height="40"
          include-users
          multiple
          @input="coworkerValueChanged($event)"
          @removeValue="removeCoworker($event)" />
      </v-card>
    </v-menu>
  </div>
</template>

<script>

export default {
  props: {
    task: {
      type: Object,
      default: () => {
        return {};
      }
    },
    globalMenu: {
      type: Boolean,
      default: false
    },
  },
  data() {
    return {
      suggestedAssignee: [],
      suggestedCoworkers: [],
      taskAssignee: {},
      taskAssigneeObj: {},
      taskCoworkerObj: [],
      taskCoworkers: [],
      currentUser: eXo.env.portal.userName,
      menu: false,
      menuId: `AssigneeMenu${parseInt(Math.random() * 10000)
        .toString()
        .toString()}`,
    };
  },
  computed: {
    suggesterLabels() {
      return {
        placeholder: this.$t('label.assignee'),
        noDataLabel: this.$t('label.noResultLabel'),
      };
    },
    searchOptions() {
      return {
        searchUrl: this.task && this.task.status && this.task.status.project ? `${eXo.env.portal.context}/${eXo.env.portal.rest}/projects/projectParticipants/`.concat(this.task.status.project.id).concat('/') :
          `${eXo.env.portal.context}/${eXo.env.portal.rest}/tasks/usersToMention/`,

      };
    }
  },
  mounted() {
    $(document).on('click', (e) => {
      if (e.target && !$(e.target).parents(`.${this.menuId}`).length && !$(e.target).parents('.v-autocomplete__content').length) {
        this.globalMenu = false;
      }
    });

  },
  created() {
    document.addEventListener('closeAssignments',()=> {
      if (this.globalMenu) {
        window.setTimeout(() => {
          this.globalMenu = false;
        }, 100);
      }
    });
    document.addEventListener('loadAssignee', event => {
      this.taskAssigneeObj = {};
      this.taskCoworkers = [];
      this.taskCoworkerObj = [];
      if (event && event.detail) {
        const task = event.detail;
        this.task = task;
        if (task.id==null && !task.status.project) {
          this.$identityService.getIdentityByProviderIdAndRemoteId('organization',this.currentUser).then(user => {
            this.taskAssigneeObj = {
              id: `organization:${user.remoteId}`,
              remoteId: user.remoteId,
              providerId: 'organization',
              profile: {
                fullName: user.profile.fullname,
                avatarUrl: user.profile.avatar,
                external: user.profile.dataEntity.external === 'true',
              },
            };
          });
        } else {
          if (task.coworker && task.coworker.length) {
            for (let i = 0; i < task.coworker.length; i++) {
              this.$identityService.getIdentityByProviderIdAndRemoteId('organization',task.coworker[i]).then(user => {
                this.taskCoworkers.push(user.remoteId);
                const taskCoworker = {
                  id: `organization:${user.remoteId}`,
                  remoteId: user.remoteId,
                  providerId: 'organization',
                  profile: {
                    fullName: user.profile.fullname,
                    avatarUrl: user.profile.avatar,
                    external: user.profile.dataEntity.external === 'true',
                  },
                };
                this.taskCoworkerObj.push(taskCoworker);
              });
            }
          }
        }
        if (task.assignee) {
          this.$identityService.getIdentityByProviderIdAndRemoteId('organization',task.assignee).then(user => {
            this.taskAssigneeObj = {
              id: `organization:${user.remoteId}`,
              remoteId: user.remoteId,
              providerId: 'organization',
              profile: {
                fullName: user.profile.fullname,
                avatarUrl: user.profile.avatar,
                external: user.profile.dataEntity.external === 'true',
              },
            };
          });
        }
      }
    });
  },
  methods: {
    assignToMe() {
      if ( this.task.assignee !== this.currentUser ) {
        this.$identityService.getIdentityByProviderIdAndRemoteId('organization',this.currentUser).then(user => {
          this.taskAssigneeObj = {
            id: `organization:${user.remoteId}`,
            remoteId: user.remoteId,
            providerId: 'organization',
            profile: {
              fullName: user.profile.fullname,
              avatarUrl: user.profile.avatar,
              external: user.profile.dataEntity.external === 'true',
            },
          };
        });
        this.$emit('updateTaskAssignment', this.currentUser);
      }
    },
    setMeAsCoworker() {
      if (this.task.id===null) {
        this.$identityService.getIdentityByProviderIdAndRemoteId('organization',this.currentUser).then(user => {
          this.taskCoworkers.push(this.currentUser);
          const taskCoworker = {
            id: `organization:${user.remoteId}`,
            remoteId: user.remoteId,
            providerId: 'organization',
            profile: {
              fullName: user.profile.fullname,
              avatarUrl: user.profile.avatar,
              external: user.profile.dataEntity.external === 'true',
            },
          };
          this.taskCoworkerObj.push(taskCoworker);
        }).then(() => {
          document.dispatchEvent(new CustomEvent('taskCoworkerChanged',{detail: this.taskCoworkers}));
          this.isNewTask = false;
        });
      } else {
        if (!this.taskCoworkers.includes(this.currentUser)) {
          this.$identityService.getIdentityByProviderIdAndRemoteId('organization',this.currentUser).then(user => {
            this.taskCoworkers.push(this.currentUser);
            const taskCoworker = {
              id: `organization:${user.remoteId}`,
              remoteId: user.remoteId,
              providerId: 'organization',
              profile: {
                fullName: user.profile.fullname,
                avatarUrl: user.profile.avatar,
                external: user.profile.dataEntity.external === 'true',
              },
            };
            this.taskCoworkerObj.push(taskCoworker);
          }).then(() => {
            this.$emit('updateTaskCoworker',this.taskCoworkers );
          });
        }
      }
    },
    assigneeValueChanged(value) {
      if (value && value.id) {
        if (value.remoteId !== this.currentUser && this.task.assignee !== value.remoteId) {
          this.taskAssigneeObj = value;
          this.$emit('updateTaskAssignment', value.remoteId);
        }
        else {
          if ( this.task.id ===null ) {
            this.$emit('updateTaskAssignment', this.taskAssigneeObj.remoteId);
          }
        }
      }
    },
    coworkerValueChanged(value) {
      if (typeof value !== 'undefined') {
        if (value && value.length && !this.taskCoworkers.includes(value[value.length-1].remoteId) ) {
          this.taskCoworkers.push(value[value.length-1].remoteId);
          this.$emit('updateTaskCoworker',this.taskCoworkers );
          this.taskCoworkerObj.push(value[value.length-1]);
        }
      }
    },
    removeCoworker(value) {
      this.taskCoworkers.splice(this.taskCoworkers.findIndex(taskCoworker => taskCoworker === value.remoteId), 1);
      this.$emit('updateTaskCoworker',this.taskCoworkers);
    },
    removeAssignee() {
      this.taskAssigneeObj = {};
      this.$emit('updateTaskAssignment', null);
    },
  }
};
</script>
