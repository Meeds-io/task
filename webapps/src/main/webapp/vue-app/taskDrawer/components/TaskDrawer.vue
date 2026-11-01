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
      <template v-if="drawer && taskId && (menuActions.length || !readOnly)" #titleIcons>
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
              <v-list-item
                v-if="!readOnly"
                class="taskActionMenu-markAsCompleted"
                @click="updateCompleted">
                <v-list-item-title class="subtitle-2">
                  <v-icon
                    color="primary"
                    class="me-2"
                    size="18">
                    mdi-check-circle-outline
                  </v-icon>
                  <span>{{ $t(task.completed ? 'message.markAsUnCompleted' : 'message.markAsCompleted') }}</span>
                </v-list-item-title>
              </v-list-item>
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
              <v-textarea
                ref="autoFocusInput4"
                v-model="taskTitle"
                :class="{taskCompleted: task && task.completed}"
                :placeholder="$t('label.tapTask.name')"
                :autofocus="!task || !task.id"
                :readonly="readOnly"
                type="text"
                class="ps-0 pt-0 task-name"
                auto-grow
                rows="1"
                row-height="13"
                required
                @change="updateTaskTitle" />
            </div>
            <div
              v-if="taskId && lastTaskChangesLog"
              class="lastUpdatedTask pb-3">
              <v-tooltip top>
                <template #activator="{ on, attrs }">
                  <button
                    v-bind="attrs"
                    type="button"
                    class="pe-2"
                    v-on="on"
                    @click="$root.$emit('displayTaskChanges')">
                    {{ $t('label.task.lastUpdate') }}
                    {{ displayedDate(lastTaskChangesLog) }}
                    {{ $t('label.task.lastUpdateBy') }}
                    {{ lastTaskChangesLogAuthor }}
                  </button>
                </template>
                <span>{{ $t('tooltip.viewAllChanges') }}</span>
              </v-tooltip>
            </div>
            <div
              id="taskDrawerFields"
              :class="twoColumns && 'taskDrawerFieldsTwoColumns'"
              class="taskDrawerFields pb-2">
              <task-drawer-field
                :label="$t('taskDrawer.field.project')"
                :pencil="!projectEditable && !readOnly"
                :pencil-label="$t('taskDrawer.menu.moveProject')"
                :editable="!readOnly"
                name="project"
                icon="fa-folder"
                @edit="startMovingProject">
                <div v-show="projectEditable && !readOnly" class="taskProjectName">
                  <task-projects
                    ref="taskProjects"
                    :task="task"
                    @blur="stopMovingProject"
                    @projectsListOpened="closePriority(); closeStatus(); closeLabelsList(); closeTaskDates();closeAssignements()" />
                </div>
                <div
                  v-if="!projectEditable"
                  class="d-flex align-center">
                  <space-avatar
                    v-if="taskSpaceId"
                    :space-id="taskSpaceId"
                    avatar
                    :size="24"
                    :popover="false"
                    extra-class="flex-shrink-0 me-2" />
                  <a
                    :href="taskProjectLink"
                    class="taskProjectLink text-body-2 text-truncate">
                    {{ taskProject.name }}
                  </a>
                </div>
              </task-drawer-field>
              <task-drawer-field
                v-show="taskProject"
                :label="$t('taskDrawer.field.status')"
                :editable="!readOnly"
                name="status"
                icon="fa-clock">
                <task-status
                  ref="taskStatus"
                  :task="task"
                  :read-only="readOnly"
                  @statusListOpened="closePriority(); closeProjectsList();closeLabelsList();closeTaskDates();closeAssignements()"
                  @updateTaskStatus="updateTaskStatus($event)" />
              </task-drawer-field>
              <task-drawer-field
                :label="$t('taskDrawer.field.assignee')"
                :pencil="hasAssigneeValue && !readOnly"
                :pencil-label="$t('taskDrawer.field.assignee')"
                :editable="!readOnly"
                name="assignee"
                icon="fa-user"
                class="taskAssignement"
                @edit="$refs.taskAssignment.openMenu()">
                <task-assignment
                  ref="taskAssignment"
                  :task="task"
                  :read-only="readOnly"
                  @updateTaskAssignment="updateTaskAssignee($event)"
                  @updateTaskCoworker="updateTaskCoworker($event)"
                  @assignmentsOpened="closePriority(); closeStatus(); closeProjectsList();closeTaskDates();closeLabelsList()" />
              </task-drawer-field>
              <task-drawer-field
                :label="$t('taskDrawer.field.priority')"
                :editable="!readOnly"
                name="priority"
                icon="fa-flag">
                <task-priority
                  ref="taskPriority"
                  :task="task"
                  :read-only="readOnly"
                  @updateTaskPriority="updateTaskPriority($event)"
                  @PriorityListOpened="closeStatus(); closeProjectsList(); closeLabelsList();closeTaskDates();closeAssignements()" />
              </task-drawer-field>
              <task-form-date-pickers
                :task="task"
                :read-only="readOnly"
                :date-picker-top="datePickerTop"
                @startDateChanged="updateTaskStartDate($event)"
                @dueDateChanged="updateTaskDueDate($event)" />
              <task-drawer-field
                v-if="taskProject"
                :label="$t('taskDrawer.field.labels')"
                :wide="twoColumns"
                name="labels"
                icon="fa-tag"
                :editable="!readOnly"
                class="taskLabelsName taskDrawerFieldWide">
                <task-labels
                  ref="taskLabels"
                  :task="task"
                  :read-only="readOnly"
                  :limit="twoColumns ? 0 : 3"
                  @labelAdded="labelsToAdd.push($event)"
                  @labelRemoved="removeLabelToAdd($event)"
                  @labelsListOpened="closePriority(); closeStatus(); closeProjectsList();closeTaskDates();closeAssignements()" />
              </task-drawer-field>
            </div>
            <div class="taskDescription py-4">
              <task-description-editor
                v-model="taskDescription"
                :task="task"
                :read-only="readOnly"
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
                creation: task.id ? null : creation,
              }"
              name="TaskDrawer"
              type="task-drawer-section"
              element="div"
              strict-type />
            <extension-registry-components
              v-if="!readOnly"
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
        :task="task" />
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
      creation: null,
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
      return `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/tasks/taskDetail/${this.task.id}`;
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
     * Whether the Assignee row holds an assignee or coworkers: the row then
     * shows its pencil, which opens the assignment menu.
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
     * Whether the task is shown read-only: a saved task stays read-only until
     * the card read says its reader may edit it, so a reader who may only view
     * it never gets an edit action or a field editor, whichever list opened
     * it. A new task, not saved yet, is always editable.
     *
     * @returns {boolean} true unless the reader may edit the task
     */
    readOnly() {
      return !!this.task && !!this.task.id && this.task.editable !== true;
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
    /**
     * Completes the task, or uncompletes it when it is completed, from the
     * drawer's menu, and tells the lists and boards.
     *
     * @returns {Promise} the update, or nothing for a task not saved yet
     */
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
    /**
     * Creates the task once every section of the drawer accepts what was
     * entered in it, then has each section write what it holds on the task
     * created. A section refusing keeps the drawer open and creates nothing;
     * a section that could not write some of it keeps the task, names what
     * was not saved, and the drawer re-opens on the task created. Once the
     * task is being created, leaving the drawer no longer drops what the
     * sections hold: they still write it, and the drawer is then closed or
     * re-opened only if it shows no other card meanwhile.
     *
     * @returns {Promise} settled once the task is created, or not
     */
    addTask() {
      if (this.saving) {
        return Promise.resolve();
      }
      this.saving = true;
      const creation = this.creation;
      return (creation ? creation.validate() : Promise.resolve(true)).then(valid => {
        if (!valid || this.creation !== creation) {
          // A refused value keeps the drawer open; a drawer closed or moved on meanwhile creates nothing
          return;
        }
        return this.createTask(creation);
      }).finally(() => this.saving = false);
    },
    /**
     * Creates the task from the drawer's fields, its labels included, then has
     * the sections write what they hold on it. The creation stays sealed
     * until then. A failure before the task exists puts the labels back in
     * the panel when it still shows that card.
     *
     * @param {object} creation the card's creation, or null
     * @returns {Promise} settled once the task is created and announced, or not
     */
    createTask(creation) {
      creation?.seal();
      document.dispatchEvent(new CustomEvent('onAddTask'));
      this.task.description = this.taskDescription;
      this.task.coworker = this.taskCoworkers;
      this.task.assignee = this.assignee;
      this.task.startDate = this.taskStartDate;
      this.task.dueDate = this.taskDueDate;
      this.task.priority = this.taskPriority;
      const labels = this.labelsToAdd;
      this.labelsToAdd = [];
      let created = false;
      return this.$taskDrawerApi.addTask(this.task)
        .then(task => {
          created = true;
          labels.forEach(item => this.$taskDrawerApi.addTaskToLabel(task.id, item));
          return this.writeCreated(task, creation);
        })
        .catch(e => {
          console.error('Error when adding task title', e);
          if (!created && this.creation === creation) {
            this.labelsToAdd = labels.concat(this.labelsToAdd);
          }
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t('alert.error')
          });
        })
        .finally(() => creation?.unseal());
    },
    /**
     * Has the sections write what they hold on the task just created, then
     * announces it.
     *
     * @param {object} task the task created
     * @param {object} creation the card's creation, or null
     * @returns {Promise} settled once the task is announced
     */
    writeCreated(task, creation) {
      return (creation ? creation.commit(task.id) : Promise.resolve([]))
        .then(notSaved => this.announceCreated(task, creation, notSaved));
    },
    /**
     * Announces the task created to the page, then closes the drawer, or
     * re-opens it on the task when a section could not write something; the
     * drawer is closed or re-opened only when it shows no other card.
     *
     * @param {object} task the task created
     * @param {object} creation the card's creation, or null
     * @param {Array<object>} notSaved what the sections could not write, each with its label
     * @returns {Promise|void} settled once the task is re-opened, when it is
     */
    announceCreated(task, creation, notSaved) {
      this.postSaveMessage({
        id: task.id,
        type: 'task'
      });
      this.$root.$emit('task-added', task);
      const shown = this.creation === creation;
      const otherCard = !shown && !!(this.creation || this.task?.id);
      if (shown) {
        this.showEditor = false;
      }
      if (notSaved.length) {
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('alert.error.task.createdValuesNotSaved', {0: notSaved.map(item => item.label).join(', ')})
        });
        return otherCard ? null : this.$tasksService.getTaskById(task.id).then(created => this.open(created));
      }
      this.$root.$emit('show-alert', {
        type: 'success',
        message: this.$t('alert.success.task.created')
      });
      if (shown) {
        this.$refs.addTaskDrawer.close();
      }
    },
    /**
     * @returns {object} a card's creation, handed to the drawer's sections:
     *          each registers what it checks before the card is created, what
     *          it writes once the card exists, and what it gives back when the
     *          drawer leaves the card. A section whose check fails for its
     *          own reasons never blocks the creation. Sealed while the card is
     *          being created, a creation writes what the sections registered
     *          when Create was confirmed, and applies the sections leaving and
     *          its giving back only once unsealed.
     */
    newCreation() {
      const hooks = new Set();
      let committed = null;
      const leaving = new Set();
      let disposeAsked = false;
      const dispose = () => {
        hooks.forEach(hook => hook.dispose?.());
        hooks.clear();
      };
      return {
        register: hook => {
          hooks.add(hook);
          return () => {
            if (committed) {
              leaving.add(hook);
            } else {
              hooks.delete(hook);
            }
          };
        },
        validate: () => Promise.all([...hooks].map(hook => Promise.resolve(hook.validate ? hook.validate() : true).catch(() => true)))
          .then(answers => answers.every(Boolean)),
        commit: taskId => Promise.all([...(committed || hooks)].map(hook => Promise.resolve(hook.commit ? hook.commit(taskId) : []).catch(() => [])))
          .then(notSaved => notSaved.flat()),
        seal: () => {
          committed = new Set(hooks);
        },
        unseal: () => {
          committed = null;
          // A section that left while the card was created is given back now: nothing else will
          leaving.forEach(hook => {
            hook.dispose?.();
            hooks.delete(hook);
          });
          leaving.clear();
          if (disposeAsked) {
            disposeAsked = false;
            dispose();
          }
        },
        dispose: () => {
          if (committed) {
            disposeAsked = true;
          } else {
            dispose();
          }
        },
      };
    },
    /**
     * Gives back the card's previous creation after the render of what the
     * drawer opens next, which the caller has already asked for by changing
     * the task: a section's own state outlives that render when it must, a
     * card re-opened on a value not saved taking over its errors.
     *
     * @param {object} previous the previous creation, or null
     * @returns {void}
     */
    disposeCreation(previous) {
      if (previous) {
        this.$nextTick(() => previous.dispose());
      }
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
    /**
     * Forgets a label removed from the panel before the task exists, so that
     * creating the task adds only the labels the panel still shows. A label
     * is matched by its id, or by its name when it is a new one with no id.
     *
     * @param {object} label the label removed from the panel
     * @returns {void}
     */
    removeLabelToAdd(label) {
      const index = this.labelsToAdd.findIndex(item => item === label
        || (label.id ? item.id === label.id : !item.id && item.name === label.name));
      if (index >= 0) {
        this.labelsToAdd.splice(index, 1);
      }
    },
    /**
     * Loads the task this drawer opens into this drawer's own fields, and in
     * no other task drawer of the page.
     *
     * @param {object} task the task the drawer opens
     * @returns {void}
     */
    loadFields(task) {
      this.$refs.taskPriority?.load(task);
      this.$refs.taskStatus?.load(task);
      this.$refs.taskProjects?.load(task);
      this.$refs.taskLabels?.load(task);
      this.$refs.taskAssignment?.load(task);
    },
    open(task) {
      const previousCreation = this.creation;
      this.creation = task.id ? null : this.newCreation();
      this.labelsToAdd = [];
      this.oldTask = task;
      this.task = JSON.parse(JSON.stringify(task));
      this.readEditable(this.task);
      this.disposeCreation(previousCreation);
      this.taskTitle = this.task.title;
      this.taskDescription = this.task.description;
      this.taskCoworkers= null;
      this.assignee= null;
      this.taskStartDate= null;
      this.taskDueDate= null;
      this.taskSpace = {};
      this.taskPriority='NONE';
      this.movingProject = false;
      // Each field is loaded through this drawer's own refs: another task
      // drawer of the page keeps its fields, and saves nothing
      window.setTimeout(() => this.loadFields(task), 200);
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
    /**
     * Reads, for a saved task opened from a list, a board or a search, whether
     * its reader may edit it: only the card read answers it, so until it does
     * the task shows read-only. The answer is applied only while the drawer
     * still shows that task.
     *
     * @param {Object} task the task the drawer shows
     * @returns {void}
     */
    readEditable(task) {
      if (!task.id || task.editable === true || task.editable === false) {
        return;
      }
      const taskId = task.id;
      this.$set(this.task, 'editable', null);
      this.$tasksService.getTaskById(taskId)
        .then(card => {
          if (this.task && this.task.id === taskId) {
            this.$set(this.task, 'editable', card.editable === true);
          }
        })
        .catch(() => {
          // A refused or failed read leaves the task read-only, and says so
          if (this.task && this.task.id === taskId) {
            this.$root.$emit('show-alert', {type: 'error', message: this.$t('alert.error')});
          }
        });
    },
    cancel() {
      this.$emit('updateTaskList');
      this.showEditor = false;
      this.$refs.addTaskDrawer.close();
    },
    onCloseDrawer() {
      this.disposeCreation(this.creation);
      this.creation = null;
      this.labelsToAdd = [];
      this.titleScrolledAway = false;
      this.$root.$emit('task-drawer-closed', this.task);
      this.showEditor = false;
      this.task = {};
      this.comments = {};
      this.$root.$emit('drawerClosed');
      this.$refs.taskLabels?.load({});
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
    /**
     * Copies the link that opens this task directly, the one the favorites
     * drawer opens, and confirms it with a « Link copied » alert: the installed
     * app has no address bar to copy it from. The alert goes through the
     * page-wide alert of the platform, since the drawer also opens outside the
     * Tasks app (a content link, the search page, the quick actions).
     *
     * @returns {Promise<void>} resolved once the alert is shown
     */
    copyLink() {
      return Promise.resolve()
        .then(() => navigator.clipboard.writeText(`${window.location.origin}${this.taskLink}`))
        .then(() => this.showPageAlert('success', this.$t('alert.success.task.linkCopied')))
        .catch(e => {
          console.error('Error when copying the task link', e);
          this.showPageAlert('error', this.$t('alert.error'));
        });
    },
    /**
     * Shows a message in the platform's page-wide alert, which every page that
     * loads the common components mounts.
     *
     * @param {string} alertType the alert's type: success, error, warning or info
     * @param {string} alertMessage the translated message
     * @returns {void}
     */
    showPageAlert(alertType, alertMessage) {
      document.dispatchEvent(new CustomEvent('alert-message', {detail: {
        alertType,
        alertMessage,
      }}));
    },
    /**
     * Builds the « Copy link » entry of the ⋯ menu, offered to whoever can open
     * the task, whatever their role in its project.
     *
     * @returns {object} the menu action
     */
    copyLinkMenuAction() {
      return {
        id: 'copyLink',
        title: this.$t('taskDrawer.menu.copyLink'),
        icon: 'fa-link',
        enabled: true,
        action: this.copyLink,
        rank: 1,
      };
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
      menuActions.push(this.copyLinkMenuAction());
      if (task && task.status && task.status.project && task.status.project.id ) {
        this.$projectService.getProject(task.status.project.id, true).then(data => {
          if (menuActionsRequest !== this.menuActionsRequest) {
            return;
          }
          this.enableDelete = data.managerIdentities.some(manager => manager.username === eXo.env.portal.userName);
          this.enableClone = this.enableDelete || data.participatorIdentities.some(participator => participator.username === eXo.env.portal.userName);
          this.addMenuAction(menuActions, this.$t('label.delete'), 'uiIconTrash', this.enableDelete, 'deleteTask', 10);
          this.addMenuAction(menuActions, this.$t('label.clone'), 'uiIconCloneNode', this.enableClone, 'cloneTask', 20);
          menuActions.sort((a, b) => a.rank - b.rank);
          this.menuActions = menuActions.filter(menuAction => menuAction.enabled === true || (typeof menuAction?.enabled === 'function' && menuAction?.enabled?.(task)));
        }).catch(() => {
          // An assignee, coworker or mentioned user outside the project opens the task without reading its project:
          // the entries that do not depend on the project stay offered
          if (menuActionsRequest !== this.menuActionsRequest) {
            return;
          }
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
      // After the click that started the move has reached the document,
      // whose outside-click handling would close the list it opens
      window.setTimeout(() => this.$refs.taskProjects?.openProjectsList(), 50);
    },

    /**
     * Once the task moved to another project, shows the Project row's link
     * again and rebuilds the ⋯ menu from the new project: the rights its
     * managers and participants give.
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
