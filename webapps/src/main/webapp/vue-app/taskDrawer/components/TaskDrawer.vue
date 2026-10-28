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
    <pinneable-drawer
      id="task-Drawer"
      app-name="task"
      :placement-disabled="!!taskId"
      ref="addTaskDrawer"
      v-model="drawer"
      :temporary="confirmDrawerClose"
      :go-back-button="addBackArrow"
      class="taskDrawer"
      body-classes="hide-scroll decrease-z-index-more"
      right
      allow-expand
      @expand-updated="expanded = $event"
      @closed="onCloseDrawer">
      <template v-if="drawer && taskId" #title>
        <div
          v-if="titleScrolledAway"
          id="taskDrawerScrolledHeader"
          class="d-flex align-center overflow-hidden">
          <v-btn
            :aria-label="$t(task.completed ? 'taskDrawer.header.markAsUncompleted' : 'taskDrawer.header.markAsCompleted')"
            class="flex-shrink-0"
            icon
            @click="updateCompleted">
            <v-icon v-if="task.completed" class="markAsCompletedBtn">mdi-checkbox-marked-circle</v-icon>
            <v-icon v-else class="markAsCompletedBtn">mdi-checkbox-blank-circle-outline</v-icon>
          </v-btn>
          <span
            :class="task.completed && 'text-decoration-line-through'"
            class="taskDrawerScrolledTitle text-truncate text-subtitle-1 font-weight-bold">
            {{ taskTitle }}
          </span>
          <v-chip
            v-if="taskStatusLabel"
            class="taskDrawerScrolledStatus flex-shrink-0 ms-2"
            color="primary"
            text-color="white"
            small>
            {{ taskStatusLabel }}
          </v-chip>
          <button
            v-if="twoColumns && lastTaskChangesLog"
            :title="$t('tooltip.viewAllChanges')"
            type="button"
            class="taskDrawerScrolledUpdate text-truncate text-body-2 text-light-color ms-3"
            @click="$root.$emit('displayTaskChanges')">
            {{ $t('label.task.lastUpdate') }}
            {{ displayedDate(lastTaskChangesLog) }}
            {{ $t('label.task.lastUpdateBy') }}
            {{ lastTaskChangesLogAuthor }}
          </button>
        </div>
        <span v-else>{{ $t('taskDrawer.header.task') }}</span>
      </template>
      <template v-else-if="drawer" #title>
        <span>{{ $t('label.drawer.header.add') }}</span>
      </template>
      <template v-if="drawer && taskId && menuActions.length" #titleIcons>
        <div
          id="taskActionMenu"
          class="position-relative z-index-modal">
          <v-menu
            v-model="displayActionMenu"
            :left="!$vuetify.rtl"
            :right="$vuetify.rtl"
            transition="slide-x-reverse-transition"
            content-class="taskActionMenu z-index-modal"
            offset-y
            attach>
            <template #activator="{on, attrs}">
              <v-btn
                v-on="on"
                v-bind="attrs"
                icon
                @click="displayActionMenu = true">
                <v-icon size="18">fa-ellipsis-v</v-icon>
              </v-btn>
            </template>
            <v-list class="pa-0 transparent" dense>
              <template v-for="menuAction in menuActions">
                <component
                  :is="menuAction.vueComponent || 'v-list-item'"
                  :key="menuAction.title"
                  :class="menuAction.id && `taskActionMenu-${menuAction.id}`"
                  v-bind="menuAction.vueComponent && {
                    task,
                  }"
                  v-on="!menuAction.vueComponent && {
                    click: menuAction.action,
                  }">
                  <v-list-item-title v-if="!menuAction.vueComponent" class="subtitle-2">
                    <i v-if="menuAction.uiIcon" :class="`uiIcon ${menuAction.uiIcon} pe-2`"></i>
                    <v-icon
                      v-else-if="menuAction.icon"
                      color="primary"
                      class="me-2"
                      size="18">
                      {{ menuAction.icon }}
                    </v-icon>
                    <span>{{ menuAction.title }}</span>
                  </v-list-item-title>
                </component>
              </template>
            </v-list>
          </v-menu>
        </div>
      </template>
      <template v-if="drawer" slot="content">
        <div
          :class="twoColumns && 'taskDrawerColumns d-flex fill-height'">
          <div
            :class="twoColumns && 'flex-grow-1 overflow-y-auto'"
            class="taskDrawerDetails pa-4">
            <div
              v-resize="fitTitleHeight"
              v-intersect.quiet="onTitleIntersect"
              class="taskTitleAndMark d-flex">
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
              v-if="taskId"
              :title="$t('tooltip.viewAllChanges')"
              class="lastUpdatedTask pb-3"
              @click="$root.$emit('displayTaskChanges')">
              <span class="pe-2">{{ $t('label.task.lastUpdate') }}
                {{ displayedDate(lastTaskChangesLog) }}
                {{ $t('label.task.lastUpdateBy') }}
                {{ lastTaskChangesLogAuthor }}</span>
            </div>
            <div
              id="taskDrawerFields"
              :class="twoColumns && 'taskDrawerFieldsTwoColumns'"
              class="taskDrawerFields pb-2">
              <task-drawer-field
                :label="$t('taskDrawer.field.project')"
                :pencil="!projectEditable"
                :pencil-label="$t('taskDrawer.menu.moveProject')"
                name="project"
                icon="fa-folder"
                editable
                @edit="startMovingProject">
                <div v-show="projectEditable" class="taskProjectName">
                  <task-projects
                    ref="taskProjects"
                    :task="task"
                    @blur="stopMovingProject"
                    @projectsListOpened="closePriority(); closeStatus(); closeLabelsList(); closeTaskDates();closeAssignements()" />
                </div>
                <a
                  v-if="!projectEditable"
                  :href="taskProjectLink"
                  class="taskProjectLink d-block text-body-2 text-truncate">
                  {{ taskProject.name }}
                </a>
              </task-drawer-field>
              <task-drawer-field
                v-show="taskProject"
                :label="$t('taskDrawer.field.status')"
                name="status"
                icon="fa-clock"
                editable>
                <task-status
                  :task="task"
                  @statusListOpened="closePriority(); closeProjectsList();closeLabelsList();closeTaskDates();closeAssignements()"
                  @updateTaskStatus="updateTaskStatus($event)" />
              </task-drawer-field>
              <task-drawer-field
                :label="$t('taskDrawer.field.assignee')"
                :pencil="hasAssigneeValue"
                name="assignee"
                icon="fa-user"
                class="taskAssignement"
                editable>
                <task-assignment
                  :task="task"
                  @updateTaskAssignment="updateTaskAssignee($event)"
                  @updateTaskCoworker="updateTaskCoworker($event)"
                  @assignmentsOpened="closePriority(); closeStatus(); closeProjectsList();closeTaskDates();closeLabelsList()" />
              </task-drawer-field>
              <task-drawer-field
                :label="$t('taskDrawer.field.priority')"
                name="priority"
                icon="fa-flag"
                editable>
                <task-priority
                  :task="task"
                  @updateTaskPriority="updateTaskPriority($event)"
                  @PriorityListOpened="closeStatus(); closeProjectsList(); closeLabelsList();closeTaskDates();closeAssignements()" />
              </task-drawer-field>
              <task-form-date-pickers
                :task="task"
                :date-picker-top="datePickerTop"
                @startDateChanged="updateTaskStartDate($event)"
                @dueDateChanged="updateTaskDueDate($event)" />
              <task-drawer-field
                v-if="taskProject"
                :label="$t('taskDrawer.field.labels')"
                :wide="twoColumns"
                name="labels"
                icon="fa-tag"
                class="taskLabelsName taskDrawerFieldWide"
                editable>
                <task-labels
                  :task="task"
                  @labelsListOpened="closePriority(); closeStatus(); closeProjectsList();closeTaskDates();closeAssignements()" />
              </task-drawer-field>
            </div>
            <v-divider class="my-0" />
            <div class="taskDescription py-4">
              <task-description-editor
                v-model="taskDescription"
                :task="task"
                :placeholder="$t('editinline.taskDescription.empty')"
                @addTaskDescription="addTaskDescription($event)" />
            </div>
            <extension-registry-components
              v-if="task"
              :key="`${task.id || 'new'}-${taskProjectId}`"
              :params="{
                taskId: task.id,
                projectId: taskProjectId,
                spaceId: taskSpaceId,
                expanded,
              }"
              name="TaskDrawer"
              type="task-drawer-section"
              element="div"
              strict-type />
            <extension-registry-components
              :params="{
                taskId: task && task.id,
                spaceId: taskSpaceId,
              }"
              name="TaskDrawer"
              type="task-drawer-action"
              element="div"
              element-class="overflow-hidden"
              class="d-flex"
              strict-type />
            <extension-registry-components
              v-if="task && task.id"
              :key="`${task.id}-${attachmentsVersion}`"
              :params="{
                entityId: String(task.id),
                entityType: 'task',
              }"
              name="TaskDrawer"
              type="task-drawer-attachments"
              element="div"
              strict-type />
            <template v-if="!twoColumns">
              <v-divider class="my-0" />
              <v-flex
                v-if="task && task.id"
                xs12
                class="pt-2 taskCommentsAndChanges">
                <task-view-all-comments
                  :task="task"
                  :comments="comments" />
              </v-flex>
            </template>
          </div>
          <aside
            v-if="twoColumns"
            class="taskDrawerAside d-flex flex-column flex-shrink-0">
            <task-comments-panel
              :task="task"
              :comments="comments"
              class="flex-grow-1"
              column
              @confirmDialogOpened="isDrawerClose = false"
              @confirmDialogClosed="isDrawerClose = true" />
          </aside>
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
    </pinneable-drawer>
    <template v-if="drawer">
      <task-comments-drawer
        v-if="!twoColumns"
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
      expanded: false,
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
      attachmentsVersion: 0,
      titleScrolledAway: false,
      movingProject: false,
    };
  },
  computed: {
    confirmDrawerClose() {
      return this.isDrawerClose;
    },
    twoColumns() {
      return this.expanded && !!this.task?.id && !this.$vuetify.breakpoint.smAndDown;
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
    /**
     * Whether the Assignee row holds an assignee or coworkers, whose names
     * are links to their profiles: the row then shows its pencil.
     *
     * @returns {boolean} whether the task has an assignee or a coworker
     */
    hasAssigneeValue() {
      return !!this.task?.assignee || !!this.task?.coworker?.length;
    },
    /**
     * Whether the Project row shows the project field: for a new task, a task with no project, or while the user moves the task.
     *
     * @returns {boolean} whether the project field shows
     */
    projectEditable() {
      return !this.taskId || !this.taskProject || this.movingProject;
    },
    /**
     * The link to the page of the task's project.
     *
     * @returns {string} the project page's URL, empty without a project
     */
    taskProjectLink() {
      return this.taskProjectId && `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/tasks/projectDetail/${this.taskProjectId}`;
    },
    /**
     * The translated name of the task's status, shown in the header once the title scrolled away.
     *
     * @returns {string} the status name, null without a status
     */
    taskStatusLabel() {
      const status = this.taskStatus?.name;
      if (!status) {
        return null;
      }
      if (this.$te(`tasks.status.${status}`)) {
        return this.$t(`tasks.status.${status}`);
      }
      return this.$te(`tasks.status.${status.toLowerCase()}`) && this.$t(`tasks.status.${status.toLowerCase()}`) || status;
    },
  },
  watch: {
    taskId() {
      if ( this.task && this.task.id !== null && typeof this.task.id !== 'undefined' ) {
        this.displayDrawerMenuAction( this.task );
      }
    },
    taskProjectId(newId, oldId) {
      if (this.drawer && newId !== oldId) {
        this.taskSpace = {};
        this.getTaskSpace();
      }
    },
    expanded() {
      this.fitTitleHeight();
    },
    twoColumns() {
      this.fitTitleHeight();
    },
  },
  created() {
    $(document).on('click', () => {
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
    document.addEventListener('entity-attachments-updated', this.refreshAttachmentsCarousel);
    this.$root.$on('update-task-project', this.onTaskProjectUpdated);
  },
  destroyed: function() {
    document.removeEventListener('keyup', this.escapeKeyListener);
    document.removeEventListener('entity-attachments-updated', this.refreshAttachmentsCarousel);
    this.$root.$off('update-task-project', this.onTaskProjectUpdated);
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
      const projectId = this.taskProjectId;
      if (projectId) {
        this.$projectService.getProject(projectId).then(
          (project) => {
            if (this.taskProjectId === projectId) {
              this.taskSpace = project.spaceDetails;
            }
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
      this.taskSpace = {};
      this.taskPriority='NONE';
      this.movingProject = false;
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
      this.getTaskSpace();
      if (task.id != null) {
        this.retrieveTaskLogs();
        this.getTaskComments();
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
      this.titleScrolledAway = false;
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
        this.$refs.addTaskDrawer.close();
      }).catch(e => {
        console.error('Error when deleting task', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('alert.error')
        });
      });
    },
    addMenuAction(menuActions, title, uiIcon, enabled, actionFunctionName, rank) {
      menuActions.push({
        title: title,
        uiIcon: uiIcon,
        enabled: enabled,
        action: this[actionFunctionName],
        rank,
      });
    },
    displayDrawerMenuAction( task ) {
      const menuActions = extensionRegistry.loadExtensions('Task', 'task-menu');
      // Only the latest rebuild applies, when the project changes again before its read returns
      const menuActionsRequest = this.menuActionsRequest = (this.menuActionsRequest || 0) + 1;
      if (task && task.status && task.status.project && task.status.project.id ) {
        this.$projectService.getProject(task.status.project.id, true).then(data => {
          if (menuActionsRequest !== this.menuActionsRequest) {
            return;
          }
          this.enableDelete = data.managerIdentities.some(manager => manager.username === eXo.env.portal.userName);
          this.enableClone = this.enableDelete || data.participatorIdentities.some(participator => participator.username === eXo.env.portal.userName);
          menuActions.push({
            id: 'moveProject',
            title: this.$t('taskDrawer.menu.moveProject'),
            icon: 'fa-folder-open',
            enabled: true,
            action: this.startMovingProject,
            rank: 5,
          });
          this.addMenuAction(menuActions, this.$t('label.delete'), 'uiIconTrash', this.enableDelete, 'deleteTask', 10);
          this.addMenuAction(menuActions, this.$t('label.clone'), 'uiIconCloneNode', this.enableClone, 'cloneTask', 20);
          menuActions.sort((a, b) => a.rank - b.rank);
          this.menuActions = menuActions.filter(menuAction => menuAction.enabled === true || (typeof menuAction?.enabled === 'function' && menuAction?.enabled?.(task)));
        });
      } else if ( task && task.id ) {
        this.enableDelete = task.createdBy === eXo.env.portal.userName;
        this.enableClone = task.assignee === eXo.env.portal.userName || this.task.coworker.includes(eXo.env.portal.userName);
        this.addMenuAction(menuActions, this.$t('label.delete'), 'uiIconTrash', this.enableDelete, 'deleteTask', 10);
        this.addMenuAction(menuActions, this.$t('label.clone'), 'uiIconCloneNode', this.enableClone, 'cloneTask', 20);
        menuActions.sort((a, b) => a.rank - b.rank);
        this.menuActions = menuActions.filter(menuAction => menuAction.enabled === true || (typeof menuAction?.enabled === 'function' && menuAction?.enabled?.(task)));
      }
    },

    /**
     * Reloads the add-on sections shown after the task's attachments (Documents'
     * row of file cards) after a file is attached to or detached from the task:
     * they read the attachments when they are created, and the attachments
     * drawer announces each change on the document.
     *
     * @returns {void}
     */
    refreshAttachmentsCarousel() {
      this.attachmentsVersion++;
    },
    /**
     * Fits the height of the task title to the lines it takes at the title's
     * new width, once the drawer has been expanded or collapsed or the window
     * resized: the title grows with its text only when the text changes, so a
     * title that wrapped in the side panel would otherwise keep its height in
     * full screen.
     *
     * @returns {void}
     */
    fitTitleHeight() {
      window.clearTimeout(this.fitTitleHeightTimeout);
      this.fitTitleHeightTimeout = window.setTimeout(() => this.$refs.autoFocusInput4?.calculateInputHeight?.(), 300);
    },
    /**
     * Shows the task's title, completion and status in the drawer's header
     * while the title is scrolled out of view, and the header's own title
     * while the title is in view.
     *
     * @param {IntersectionObserverEntry[]} entries the observed changes
     * @param {IntersectionObserver} observer the observer of the title
     * @param {boolean} isIntersecting whether the title is in view
     * @returns {void}
     */
    onTitleIntersect(entries, observer, isIntersecting) {
      this.titleScrolledAway = !!this.taskId && !isIntersecting;
    },

    /**
     * Swaps the Project row's link for the project field and opens its list,
     * so that the user picks the project the task moves to.
     *
     * @returns {void}
     */
    startMovingProject() {
      this.movingProject = true;
      // After the menu item's click has reached the document, whose
      // outside-click handling would close the list it opens
      window.setTimeout(() => this.$refs.taskProjects?.openProjectsList(), 50);
    },

    /**
     * Once the task moved to another project, shows the Project row's link
     * again and rebuilds the ⋯ menu from the new project: its move action and
     * the rights its managers and participants give.
     *
     * @returns {void}
     */
    onTaskProjectUpdated() {
      this.stopMovingProject();
      if (this.taskId) {
        this.displayDrawerMenuAction(this.task);
      }
    },

    /**
     * Shows the Project row's link again once the task moved, or once the
     * project field lost the focus without a move. The delay lets a click on
     * a project of the list reach it before the field is removed.
     *
     * @returns {void}
     */
    stopMovingProject() {
      if (this.movingProject) {
        window.clearTimeout(this.stopMovingProjectTimeout);
        this.stopMovingProjectTimeout = window.setTimeout(() => this.movingProject = false, 400);
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
