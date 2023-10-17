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
  <div>
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :message="deleteConfirmMessage"
      :title="$t('popup.deleteTask')"
      :ok-label="$t('label.ok')"
      :cancel-label="$t('popup.cancel')"
      @ok="deleteConfirm()" />
    <exo-drawer
      id="task-Drawer"
      ref="addTaskDrawer"
      v-model="drawer"
      :temporary="confirmDrawerClose"
      class="taskDrawer"
      body-classes="hide-scroll decrease-z-index-more"
      right
      @closed="onCloseDrawer">
      <template
        v-if="drawer && task?.id"
        slot="title">
        <div class="drawerTitleAndProject d-flex">
          <i
            v-if="addBackArrow"
            class="uiIcon uiArrowBAckIcon"
            @click="closeTaskDrawer"></i>
          <span>{{ $t('label.project') }}</span>
          <div class="taskProjectName">
            <task-projects
              :task="task"
              @projectsListOpened="closePriority(); closeStatus(); closeLabelsList(); closeTaskDates();closeAssignements()" />
          </div>
        </div>
        <div v-if="menuActions.length" id="taskActionMenu">
          <i class="uiIcon uiThreeDotsIcon" @click="displayActionMenu = true"></i>
          <v-menu
            v-model="displayActionMenu"
            :attach="'#taskActionMenu'"
            transition="slide-x-reverse-transition"
            content-class="taskActionMenu"
            offset-y>
            <v-list class="pa-0" dense>
              <v-list-item v-for="menuAction in menuActions" :key="menuAction.title">
                <v-list-item-title class="subtitle-2" @click="menuAction.action">
                  <i :class="`uiIcon ${menuAction.uiIcon} pe-2`"></i>
                  <span>{{ menuAction.title }}</span>
                </v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
        </div>
      </template>
      <template v-else-if="drawer" slot="title">
        <div class="drawerTitleAndProject d-flex">
          <i
            v-if="addBackArrow"
            class="uiIcon uiArrowBAckIcon"
            @click="closeTaskDrawer"></i>
          <span>{{ $t('label.drawer.header.add') }}</span>
          <div class="taskProjectName">
            <task-projects
              :task="task"
              @projectsListOpened="closePriority(); closeStatus(); closeLabelsList(); closeTaskDates();closeAssignements()" />
          </div>
        </div>
      </template>
      <template v-if="drawer" slot="content">
        <div class="taskDrawerDetails pa-4">
          <div class="taskTitleAndMark d-flex">
            <v-btn
              id="check_btn"
              class="ms-n2"
              icon
              dark
              @click="updateCompleted">
              <v-icon v-if="task && task.completed" class="markAsCompletedBtn">mdi-checkbox-marked-circle</v-icon>
              <v-icon v-else class="markAsCompletedBtn">mdi-checkbox-blank-circle-outline</v-icon>
            </v-btn>
            <v-textarea
              ref="autoFocusInput4"
              v-model="taskTitle"
              :class="{taskCompleted: task && task.completed}"
              :placeholder="$t('label.tapTask.name')"
              :autofocus="!task || !task.id"
              type="text"
              class="ps-0 pt-0 task-name"
              auto-grow
              rows="1"
              row-height="13"
              required
              @change="updateTaskTitle" />
          </div>
          <div
            v-if="task && task.id"
            :title="$t('tooltip.viewAllChanges')"
            class="lastUpdatedTask pb-3"
            @click="$root.$emit('displayTaskChanges')">
            <span class="pe-2">{{ $t('label.task.lastUpdate') }}
              {{ displayedDate(lastTaskChangesLog) }}
              {{ $t('label.task.lastUpdateBy') }}
              {{ lastTaskChangesLogAuthor }}</span>
          </div>
          <div class="taskAssignement ms-8 pb-3">
            <task-assignment
              :task="task"
              @updateTaskAssignment="updateTaskAssignee($event)"
              @updateTaskCoworker="updateTaskCoworker($event)"
              @assignmentsOpened="closePriority(); closeStatus(); closeProjectsList();closeTaskDates();closeLabelsList()" />
          </div>
          <v-divider class="my-0" />
          <div class="d-flex py-2">
            <div class="taskDates">
              <task-form-date-pickers
                :task="task"
                :date-picker-top="datePickerTop"
                @startDateChanged="updateTaskStartDate($event)"
                @dueDateChanged="updateTaskDueDate($event)" />
            </div>
            <div class="taskStatusAndPriority">
              <task-priority
                :task="task"
                class="py-2"
                @updateTaskPriority="updateTaskPriority($event)"
                @PriorityListOpened="closeStatus(); closeProjectsList(); closeLabelsList();closeTaskDates();closeAssignements()" />
              <task-status
                :task="task"
                class="py-2"
                @statusListOpened="closePriority(); closeProjectsList();closeLabelsList();closeTaskDates();closeAssignements()"
                @updateTaskStatus="updateTaskStatus($event)" />
            </div>
          </div>
          <v-divider class="my-0" />
          <div class="taskDescription py-4">
            <task-description-editor
              v-model="taskDescription"
              :task="task"
              :placeholder="$t('editinline.taskDescription.empty')"
              @addTaskDescription="addTaskDescription($event)" />
          </div>
          <div class="taskLabelsName mt-3 mb-3">
            <task-labels
              v-if="task && task.status && task.status.project"
              :task="task"
              @labelsListOpened="closePriority(); closeStatus(); closeProjectsList();closeTaskDates();closeAssignements()" />
          </div>
          <extension-registry-components
            :params="{
              taskId: task && task.id,
              spaceId: taskSpaceId,
            }"
            name="TaskDrawer"
            type="task-drawer-action"
            element="div"
            element-class="overflow-hidden"
            class="d-flex" />
          <v-divider class="my-0" />
          <v-flex
            v-if="task && task.id"
            xs12
            class="pt-2 taskCommentsAndChanges">
            <task-view-all-comments
              :task="task"
              :comments="comments" />
          </v-flex>
        </div>
      </template>
      <template v-if="task && !task.id" slot="footer">
        <div class="d-flex">
          <v-spacer />
          <v-btn
            class="btn me-2"
            @click="cancel">
            {{ $t('popup.cancel') }}
          </v-btn>
          <v-btn
            :disabled="disableSaveButton"
            class="btn btn-primary"
            @click="addTask">
            {{ $t('label.save') }}
          </v-btn>
        </div>
      </template>
    </exo-drawer>
    <template v-if="drawer">
      <task-comments-drawer
        ref="taskCommentDrawer"
        :task="task"
        :comments="comments"
        @confirmDialogOpened="isDrawerClose = false"
        @confirmDialogClosed="isDrawerClose = true" />
      <task-changes-drawer
        ref="taskChangesDrawer"
        :task="task"
        :logs="logs" />
      <attachments-image-crop-drawer />
      <attachments-image-preview-dialog />
    </template>
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
  },
  data() {
    return {
      displayActionMenu: false,
      menuActions: [],
      drawer: false,
      reset: false,
      dates: [],
      commentPlaceholder: this.$t('comment.message.addYourComment'),
      descriptionPlaceholder: this.$t('editinline.taskDescription.empty'),
      chips: [],
      autoSaveDelay: 1000,
      saveDescription: '',
      logs: [],
      comments: [],
      subEditorIsOpen: false,
      taskPriority: 'NONE',
      labelsToAdd: [],
      assignee: null,
      taskCoworkers: [],
      taskDueDate: null,
      taskStartDate: null,
      saving: false,
      deleteConfirmMessage: null,
      datePickerTop: true,
      enableDelete: false,
      enableClone: false,
      currentUserName: eXo.env.portal.userName,
      MESSAGE_MAX_LENGTH: 2000,
      dateTimeFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      },
      lang: eXo.env.portal.language,
      commentId: '',
      isDrawerClose: true,
      oldTask: {},
      showBackArrow: false,
      taskSpace: {},
      taskTitle: null,
      taskDescription: null,
    };
  },
  computed: {
    confirmDrawerClose() {
      return this.isDrawerClose;
    },
    taskLink() {
      if (this.task == null || this.task.id == null) {
        return '';
      }
      return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/tasks/taskDetail/${this.task.id}`;
    },
    taskTitleValid() {
      return  this.taskTitle?.trim().length >= 3 && this.taskTitle.length < 1024;
    },
    taskDescriptionValid() {
      const taskDescriptionText = this.$utils.htmlToText(this.taskDescription);
      return !taskDescriptionText || taskDescriptionText.length <= 2000;
    },
    disableSaveButton() {
      return this.saving || !this.taskTitleValid || !this.taskDescriptionValid;
    },
    lastTaskChangesLog() {
      return this.logs && this.logs.length && this.logs[0].createdTime || '';
    },
    lastTaskChangesLogAuthor() {
      return this.logs && this.logs.length && ( this.logs[0].external ? `${this.logs[0].authorFullName} (${this.$t('label.external')})`  || '' : this.logs[0].authorFullName || '');
    },
    taskId() {
      return this.task && this.task.id;
    },
    addBackArrow() {
      return this.showBackArrow;
    },
    taskStatus() {
      return this.task && this.task.status;
    },
    taskProject() {
      return this.taskStatus && this.taskStatus.project;
    },
    taskProjectId() {
      return this.taskProject && this.taskProject.id;
    },
    taskSpaceId() {
      return this.taskSpace && this.taskSpace.id;
    },
  },
  watch: {
    taskId() {
      if ( this.task && this.task.id !== null && typeof this.task.id !== 'undefined' ) {
        this.displayDrawerMenuAction( this.task );
      }
    }
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.displayActionMenu) {
        window.setTimeout(() => {
          this.displayActionMenu = false;
        }, 200);
      }
    });
    document.addEventListener('labelListChanged', event => {
      if (event && event.detail) {
        const label = event.detail;
        this.labelsToAdd.push(label);
      }
    });
    document.addEventListener('taskAssigneeChanged', event => {
      if (event && event.detail) {
        this.assignee = event.detail;
      }
    });
    document.addEventListener('taskCoworkerChanged', event => {
      if (event && event.detail) {
        this.taskCoworkers = event.detail;
      }
    });
    this.$root.$on('display-back-arrow', () => {
      this.showBackArrow = true;
    });
  },
  destroyed: function() {
    document.removeEventListener('keyup', this.escapeKeyListener);
  },
  methods: {

    closePriority() {
      document.dispatchEvent(new CustomEvent('closePriority'));
    },
    closeStatus() {
      document.dispatchEvent(new CustomEvent('closeStatus'));
    },
    closeProjectsList() {
      document.dispatchEvent(new CustomEvent('closeProjectList'));
    },
    closeLabelsList() {
      document.dispatchEvent(new CustomEvent('closeLabelsList'));
    },
    closeTaskDates() {
      document.dispatchEvent(new CustomEvent('closeDates'));
    },
    closeAssignements() {
      document.dispatchEvent(new CustomEvent('closeAssignments'));
    },
    updateTaskTitle() {
      if (this.task.id && this.oldTask.title !== this.taskTitle) {
        if (!this.taskTitle || !this.taskTitle.length) {
          this.$root.$emit('show-alert', {type: 'error',message: this.$t('alert.error.title.mandatory')});
        } else if (!this.taskTitleValid) {
          this.$root.$emit('show-alert', {type: 'error',message: this.$t('alert.error.title.length')});
        } else {
          const task = JSON.parse(JSON.stringify(this.task));
          task.title = this.taskTitle;
          this.$taskDrawerApi.updateTask(this.task.id, task)
            .then(() => {
              this.task.title = this.oldTask.title = this.taskTitle;
              this.$root.$emit('show-alert', {
                type: 'success',
                message: this.$t('alert.success.task.title')
              });
              this.$root.$emit('task-updated', JSON.parse(JSON.stringify(this.task)));
            }).catch(e => {
              console.error('Error when updating task\'s title', e);
              this.$root.$emit('show-alert', {
                type: 'error',
                message: this.$t('alert.error')
              });
            });
        }
      } else if (!this.task.id) {
        this.task.title = this.oldTask.title = this.taskTitle;
      }
    },
    updateCompleted() {
      const task = {
        id: this.task.id,
        isCompleted: !this.task.completed,
      };
      if (task.id) {
        return this.$tasksService.updateCompleted(task).then(updatedTask => {
          if (updatedTask.completed){
            this.$root.$emit('show-alert', {type: 'success',message: this.$t('alert.success.task.completed')});
          } else {
            this.$root.$emit('show-alert', {type: 'success',message: this.$t('alert.success.task.unCompleted')});
          }
          this.$root.$emit('update-task-completed', updatedTask);
        }).then(() => this.task.completed = task.isCompleted)
          .then( () => {
            this.$root.$emit('update-completed-task',this.task.completed,this.task.id);
          })
          .catch(e => {
            console.error('Error updating project', e);
            this.$root.$emit('show-alert', {
              type: 'error',
              message: this.$t('alert.error')
            });
            this.postProject = false;
          });
      }
    },
    updateTaskPriority(value) {
      if (value.priority && this.oldTask.priority !== value.priority) {
        if (this.task.id != null) {
          this.task.priority = value.priority;
          this.$taskDrawerApi.updateTask(this.task.id, this.task).then( () => {
            this.oldTask.priority = this.task.priority;
            this.$root.$emit('task-updated', this.task);
            this.$root.$emit('task-priority-updated',this.task);
            this.$root.$emit('show-alert', {
              type: 'success',
              message: this.$t('alert.success.task.priority')
            });
          }).catch(e => {
            console.error('Error when updating task\'s priority', e);
            this.$root.$emit('show-alert', {
              type: 'error',
              message: this.$t('alert.error')
            });
          });
        } else {
          this.taskPriority = value.priority;
        }
      }
    },
    updateTaskStatus(value) {
      if (value && this.oldTask?.status?.id !== value?.id) {
        if (this.task.id != null) {
          this.task.status = value;
          this.$taskDrawerApi.updateTask(this.task.id, this.task).then( () => {
            this.oldTask.status = this.task.status;
            this.$root.$emit('task-status-updated',this.task);
            this.$root.$emit('show-alert', {
              type: 'success',
              message: this.$t('alert.success.task.status')
            });
          }).catch(e => {
            console.error('Error when updating task\'s status', e);
            this.$root.$emit('show-alert', {
              type: 'error',
              message: this.$t('alert.error')
            });
          });
        }
      }
    },
    updateTaskStartDate(value) {
      if (value !== 'none' && value.date === 1 && value.month === 0 && value.timezoneOffset < 0) {
        value.year += 1;
      }
      if (value && value !== 'none'  && (!this.oldTask.startDate|| !this.datesEquals(this.oldTask.startDate,value))) {
        if (this.task.id != null) {
          this.task.startDate = value;
          this.$taskDrawerApi.updateTask(this.task.id, this.task).then( () => {
            this.oldTask.startDate = this.task.startDate;
            this.$root.$emit('task-updated', this.task);
            this.$root.$emit('task-start-date-updated', this.task);
            this.$root.$emit('show-alert', {
              type: 'success',
              message: this.$t('alert.success.task.startDate')
            });
          }).catch(e => {
            console.error('Error when updating task\'s start date', e);
            this.$root.$emit('show-alert', {
              type: 'error',
              message: this.$t('alert.error')
            });
          });
        } else {
          this.taskStartDate = value;
        }
      } else if (value === 'none' && this.oldTask.startDate) {
        this.task.startDate = null;
        this.$taskDrawerApi.updateTask(this.task.id, this.task).then(() => {
          this.$root.$emit('show-alert', {
            type: 'success',
            message: this.$t('alert.success.task.startDate')
          });
          this.$root.$emit('task-start-date-updated', this.task);
        }).catch(e => {
          console.error('Error has occurred while removing the start date ' , e);
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t('alert.error')
          });
        });
      }
    },
    updateTaskDueDate(value) {
      if (value !== 'none' && value.date === 1 && value.month === 0 && value.timezoneOffset < 0) {
        value.year += 1;
      }
      if (value && value !== 'none' && (!this.oldTask.dueDate || !this.datesEquals(this.oldTask.dueDate,value))) {
        if (this.task.id !== null) {
          this.task.dueDate = value;
          this.$taskDrawerApi.updateTask(this.task.id, this.task).then(() => {
            this.oldTask.dueDate = this.task.dueDate;
            this.$root.$emit('task-updated', this.task);
            this.$root.$emit('task-due-date-updated', this.task);
            this.$root.$emit('show-alert', {
              type: 'success',
              message: this.$t('alert.success.task.duetDate')
            });
            this.$root.$emit('update-task-widget-list', this.task);
          }).catch(e => {
            this.logError(e);
          }).finally(() => this.$root.$emit('task-due-date-updated', this.task));
        } else {
          this.taskDueDate = value;
        }
      } else if (value === 'none' && this.oldTask.dueDate) {
        this.task.dueDate = null;
        this.$taskDrawerApi.updateTask(this.task.id, this.task).then(() => {
          this.oldTask.dueDate = this.task.dueDate;
          this.$root.$emit('show-alert', {
            type: 'success',
            message: this.$t('alert.success.task.duetDate')
          });
        }).catch(e => {
          this.logError(e);
        }).finally(() => this.$root.$emit('task-due-date-updated', this.task));
      }
    },
    postSaveMessage(task) {
      const postSaveOperations = extensionRegistry.loadExtensions('task', 'saveAction');
      if (postSaveOperations?.length) {
        const promises = [];
        postSaveOperations.forEach(extension => {
          if (extension.postSave) {
            const result = extension.postSave(task);
            if (result?.then) {
              promises.push(result);
            }
          }
        });
        return Promise.all(promises).then(() => task);
      }
    },
    addTask() {
      document.dispatchEvent(new CustomEvent('onAddTask'));
      this.task.description = this.taskDescription;
      this.task.coworker = this.taskCoworkers;
      this.task.assignee = this.assignee;
      this.task.startDate = this.taskStartDate;
      this.task.dueDate = this.taskDueDate;
      this.task.priority = this.taskPriority;
      this.$taskDrawerApi.addTask(this.task).then(task => {
        this.labelsToAdd.forEach(item => {
          this.$taskDrawerApi.addTaskToLabel(task.id, item);
        });
        const taskObject = {
          id: task.id,
          type: 'task'
        };
        this.postSaveMessage(taskObject);
        this.$root.$emit('task-added', task);
        this.$root.$emit('show-alert', {
          type: 'success',
          message: this.$t('alert.success.task.created')
        });
        this.showEditor = false;
        this.$refs.addTaskDrawer.close();
        this.labelsToAdd = [];
      }).catch(e => {
        console.error('Error when adding task title', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('alert.error')
        });
      });
    },
    updateTaskAssignee(value) {
      if (this.task.id !== null && this.oldTask.assignee!==value) {
        if (value) {
          this.task.assignee = value;
        } else {
          this.task.assignee = null;
        }
        this.$taskDrawerApi.updateTask(this.task.id, this.task).then( () => {
          this.oldTask.assignee=this.task.assignee;
          this.$root.$emit('show-alert', {
            type: 'success',
            message: this.$t('alert.success.task.assignee')
          });
        }).then( () => {
          this.$root.$emit('update-task-assignee',value,this.task.id);
        }).catch(e => {
          console.error('Error when updating task\'s assignee', e);
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t('alert.error')
          });
        });
      } else {
        if (value) {
          this.assignee = value;
        } else {
          this.assignee = null;
        }
      }
    },
    updateTaskCoworker(value) {
      if (this.task.id !== null) {
        if (value && value.length) {
          this.task.coworker = value;
        } else {
          this.task.coworker = [];
        }
        this.$taskDrawerApi.updateTask(this.task.id, this.task).then( () => {
          this.$root.$emit('show-alert', {
            type: 'success',
            message: this.$t('alert.success.task.coworker')
          });
        }).then( () => {
          this.$root.$emit('update-task-coworker',value,this.task.id);
        }).catch(e => {
          console.error('Error when updating task\'s coworkers', e);
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t('alert.error')
          });
        });
      } else {
        if (value && value.length) {
          this.taskCoworkers = value;
        } else {
          this.taskCoworkers = [];
        }
      }
    },
    addTaskDescription(value) {
      this.taskDescription = value;
    },
    retrieveTaskLogs() {
      this.$taskDrawerApi.getTaskLogs(this.task.id).then(
        (data) => {
          this.logs = data;
        });
      return this.logs;
    },
    getTaskComments() {
      this.$taskDrawerApi.getTaskComments(this.task.id).then(
        (data) => {
          this.comments = data;
        });
      return this.comments;
    },
    getTaskSpace() {
      if (this.taskProjectId) {
        this.$projectService.getProject(this.taskProjectId).then(
          (project) => {
            this.taskSpace = project.spaceDetails;
          });
      } else {
        this.taskSpace = {};
      }
    },
    navigateTo(pagelink) {
      window.open(`${ eXo.env.portal.context }/${ eXo.env.portal.portalName }/${ pagelink }`, '_blank');
    },
    open(task) {
      this.oldTask = task;
      this.task = JSON.parse(JSON.stringify(task));
      this.taskTitle = this.task.title;
      this.taskDescription = this.task.description;
      this.taskCoworkers= null;
      this.assignee= null;
      this.taskStartDate= null;
      this.taskDueDate= null;
      this.taskPriority='NONE';
      window.setTimeout(() => {
        document.dispatchEvent(new CustomEvent('loadTaskPriority', {
          detail: task
        }));
        document.dispatchEvent(new CustomEvent('loadProjectStatus', {
          detail: task
        }));
        document.dispatchEvent(new CustomEvent('loadProjectName', {
          detail: task
        }));
        document.dispatchEvent(new CustomEvent('loadPlanDates', {
          detail: task
        }));
        document.dispatchEvent(new CustomEvent('loadProjectLabels', {
          detail: task
        }));
        document.dispatchEvent(new CustomEvent('loadTaskLabels', {
          detail: task
        }));
        document.dispatchEvent(new CustomEvent('loadAssignee', {
          detail: task
        }));
      },
      200);
      if (task.id != null) {
        this.retrieveTaskLogs();
        this.getTaskComments();
        this.getTaskSpace();
        this.$root.$emit('set-url', {
          type: 'task',
          id: task.id
        });
      }
      this.$refs.addTaskDrawer.open();
    },
    cancel() {
      this.$emit('updateTaskList');
      this.showEditor = false;
      this.$refs.addTaskDrawer.close();
    },
    onCloseDrawer() {
      this.$root.$emit('task-drawer-closed', this.task);
      this.showEditor = false;
      this.task = {};
      this.comments = {};
      this.$root.$emit('drawerClosed');
      document.dispatchEvent(new CustomEvent('loadTaskLabels', {
        detail: {}
      }));
      this.$root.$emit('hideTaskComment');
      this.$root.$emit('hideTaskChanges');
    },
    deleteTask() {
      this.deleteConfirmMessage = `${this.$t('popup.msg.deleteTask')} : <strong>${this.task.title}</strong>? `;
      this.$refs.deleteConfirmDialog.open();
    },
    cloneTask() {
      this.$taskDrawerApi.cloneTask(this.task.id).then(task => {
        this.$root.$emit('show-alert', {
          type: 'success',
          message: this.$t('alert.success.task.cloned') 
        });
        this.$root.$emit('task-added', task);
        this.$root.$emit('open-task-drawer', task);
      }).catch(e => {
        console.error('Error when cloning task', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('alert.error')
        });
      });
    },
    deleteConfirm() {
      return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/tasks/${this.task.id}`, {
        method: 'DELETE',
        credentials: 'include',
      }).then( () => {
        this.$root.$emit('deleteTask', {
          detail: this.task.id
        });
        this.$root.$emit('show-alert', {
          type: 'success',
          message: this.$t('alert.success.task.deleted') 
        });
        document.dispatchEvent(new CustomEvent('deleteTask', {
          detail: this.task.id
        }));
      }).catch(e => {
        console.error('Error when deleting task', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('alert.error')
        });
      });
    },
    addMenuAction(title, uiIcon, enabled, actionFunctionName) {
      this.menuActions.push({
        title: title,
        uiIcon: uiIcon,
        enabled: enabled,
        action: this[actionFunctionName]
      });
    },
    displayDrawerMenuAction( task ) {
      this.menuActions = [];
      if (task && task.status && task.status.project && task.status.project.id ) {
        this.$projectService.getProject(task.status.project.id, true).then(data => {
          this.enableDelete = data.managerIdentities.some(manager => manager.username === eXo.env.portal.userName);
          this.enableClone = this.enableDelete || data.participatorIdentities.some(participator => participator.username === eXo.env.portal.userName);
          this.addMenuAction(this.$t('label.delete'), 'uiIconTrash', this.enableDelete, 'deleteTask');
          this.addMenuAction(this.$t('label.clone'), 'uiIconCloneNode', this.enableClone, 'cloneTask');
          this.menuActions = this.menuActions.filter(menuAction => menuAction.enabled);
        });
      } else if ( task && task.id ) {
        this.enableDelete = task.createdBy === eXo.env.portal.userName;
        this.enableClone = task.assignee === eXo.env.portal.userName || this.task.coworker.includes(eXo.env.portal.userName);
        this.addMenuAction(this.$t('label.delete'), 'uiIconTrash', this.enableDelete, 'deleteTask');
        this.addMenuAction(this.$t('label.clone'), 'uiIconCloneNode', this.enableClone, 'cloneTask');
        this.menuActions = this.menuActions.filter(menuAction => menuAction.enabled);
      }
    },

    displayedDate(value) {
      return value && this.$dateUtil.formatDateObjectToDisplay(new Date(value), this.dateTimeFormat, this.lang) || '';
    },
    openCommentDrawer() {
      this.$root.$emit('displayTaskComment', this.comments[this.comments.length-1].comment.id, true);
    },
    datesEquals(date1,date2){
      if (date1.month===date2.month&&date1.year===date2.year&&date1.date===date2.date){
        return true;
      }
      return false;
    },
    closeTaskDrawer() {
      this.$refs.addTaskDrawer.close();
      this.$root.$emit('displayUnscheduledDrawer', '');
      this.showBackArrow = false;    
    },
    logError(e) {
      console.error('Error when updating task\'s due date', e);
      this.$root.$emit('show-alert', {
        type: 'error',
        message: this.$t('alert.error')
      });
    },
  }
};
</script>
