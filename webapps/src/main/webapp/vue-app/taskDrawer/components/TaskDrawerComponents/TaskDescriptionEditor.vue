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
    <div
      id="taskDescriptionId"
      :class="editorReady && 'active'"
      class="taskDescription richEditor">
      <div class="taskDescriptionToShow">
        <div
          v-sanitized-html="value"
          :data-text="placeholder"
          :title="$t('tooltip.clickToEdit')"
          contentEditable="true"
          class="py-1 px-2 reset-style-box rich-editor-content"
          @click="showDescriptionEditor($event)">
          {{ placeholder }}
        </div>
        <attachments-image-items
          :object-id="metadataObjectId"
          object-type="task"
          :preview-width="250"
          :preview-height="250" />
      </div>
      <rich-editor
        v-if="editorReady"
        ref="taskDescriptionEditor"
        v-model="value"
        id="descriptionContent"
        :max-length="MESSAGE_MAX_LENGTH"
        :placeholder="placeholder"
        :object-id="metadataObjectId"
        :tag-enabled="false"
        ck-editor-type="taskContent"
        object-type="task"
        autofocus />
    </div>
    <v-btn
      v-if="task.id && displayEditor && editorReady"
      id="saveDescriptionButton"
      :loading="savingDescription"
      :disabled="saveDescriptionButtonDisabled"
      depressed
      outlined
      class="btn mt-2 ml-auto d-flex px-2 btn-primary v-btn v-btn--contained theme--light v-size--default"
      @click="saveDescription($event)">
      {{ $t('label.apply') }}
    </v-btn>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    },
    task: {
      type: Object,
      default: function () {
        return {};
      }
    }
  },
  data() {
    return {
      MESSAGE_MAX_LENGTH: 2000,
      inputVal: '',
      editorReady: false,
      showEditor: false,
      savingDescription: false
    };
  },
  computed: {
    charsCount() {
      const pureText = this.$utils.htmlToText(this.value);
      return pureText.length;
    },
    saveDescriptionButtonDisabled() {
      return this.savingDescription || this.charsCount > this.MESSAGE_MAX_LENGTH;
    },
    taskDescription() {
      return this.task?.description || '';
    },
    displayEditor() {
      return this.showEditor;
    },
    metadataObjectId() {
      return this.task?.id || null;
    }
  },
  watch: {
    inputVal(val) {
      const editorData = this.$refs.taskDescriptionEditor && this.$refs.taskDescriptionEditor.getMessage();
      if (editorData != null && val !== editorData) {
        if (val === '') {
          this.$refs.taskDescriptionEditor.initCKEditorData('');
          this.$refs.taskDescriptionEditor.initCKEditor();
        } else {
          this.$refs.taskDescriptionEditor.initCKEditorData(val);
        }
      }
      if (this.editorReady) {
        this.$emit('input', val);
      }
    },
    editorReady(val) {
      if (val === true) {
        if (this.$refs.taskDescriptionEditor) {
          this.$refs.taskDescriptionEditor.initCKEditor();
        }
        document.getElementById('taskDescriptionId').classList.remove('taskDescription');
        if (this.$refs.taskDescriptionEditor) {
          this.$refs.taskDescriptionEditor.setFocus(true);
        }
      } else {
        document.getElementById('taskDescriptionId').classList.add('taskDescription');
      }
    },
    reset() {
      this.hideDescriptionEditor();
    },
  },
  created() {
    this.$root.$off('drawerClosed');
    this.$root.$on('drawerClosed', () => {
      this.hideDescriptionEditor();
    });
    document.addEventListener('onAddTask', () => {
      this.$emit('addTaskDescription',this.value);
      this.showEditor = false;
    });
    this.inputVal = this.value || '';
    $('#task-Drawer').on('click', (event) => {
      if (this.showEditor && event?.target && !$(event.target).parents('#taskDescriptionId').length) {
        this.hideDescriptionEditor();
      }
    });
  },
  methods: {
    saveDescription(event) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      const newValue = this.value?.replace('&nbsp;',' ');
      if (this.task.id && !isNaN(this.task.id)) {
        this.savingDescription = true;

        const task = JSON.parse(JSON.stringify(this.task));
        task.description = newValue;
        this.$taskDrawerApi.updateTask(this.task.id , task)
          .then(() => {
            this.task.description = newValue;
            this.$refs.taskDescriptionEditor.saveAttachments();
            this.$root.$emit('show-alert', {
              type: 'success',
              message: this.$t('alert.success.task.description')
            });
            this.$root.$emit('task-description-updated', this.task);
            this.hideDescriptionEditor();
          }).catch(() => {
            this.$root.$emit('show-alert',{
              type: 'error',
              message: this.$t('alert.error')
            });
          }).finally(() => this.savingDescription = false);
      }
    },
    hideDescriptionEditor() {
      if (this.$refs.taskDescriptionEditor) {
        this.$refs.taskDescriptionEditor.destroyCKEditor();
      }
      this.editorReady = false;
      this.showEditor = false;
    },
    showDescriptionEditor: function (event) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      if (!this.showEditor && event?.target?.nodeName !== 'A') {
        this.inputVal = this.value;
        this.editorReady = true;
        this.showEditor = true;
      } else if (event?.target?.nodeName === 'A') {
        window.open(event.target.href, '_blank');
      }
    },
  }
};
</script>