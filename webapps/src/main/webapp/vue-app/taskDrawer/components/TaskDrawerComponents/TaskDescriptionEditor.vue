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
  <div class="taskDescriptionEditor position-relative">
    <v-btn
      v-if="!showEditor && value && !readOnly"
      :aria-label="$t('taskDrawer.description.edit')"
      class="taskDescriptionPen"
      icon
      @click="showDescriptionEditor($event)">
      <v-icon size="12" class="text-light-color">fa-pen</v-icon>
    </v-btn>
    <div
      id="taskDescriptionId"
      :class="editorReady && 'active'"
      class="taskDescription richEditor">
      <div class="taskDescriptionToShow">
        <button
          v-if="!value && !readOnly"
          type="button"
          class="taskDescriptionInvite width-full text-start py-1 px-2"
          @click="showDescriptionEditor($event)">
          {{ placeholder }}
        </button>
        <div
          v-show="value"
          ref="descriptionText"
          v-sanitized-html="value"
          :class="!expanded && 'taskDescriptionClamped'"
          class="taskDescriptionText py-1 px-2 reset-style-box rich-editor-content"
          @click="onDescriptionClick($event)">
        </div>
        <v-btn
          v-if="clamped || expanded"
          :aria-expanded="String(expanded)"
          class="taskDescriptionMore text-none px-2"
          color="primary"
          text
          @click="expanded = !expanded">
          {{ $t(expanded ? 'taskDrawer.description.seeLess' : 'taskDrawer.description.seeMore') }}
        </v-btn>
        <attachments-image-items
          v-if="metadataObjectId"
          :object-id="metadataObjectId"
          :preview-width="250"
          :preview-height="250"
          object-type="task" />
      </div>
      <rich-editor
        v-if="editorReady"
        ref="taskDescriptionEditor"
        v-model="description"
        id="descriptionContent"
        :max-length="MESSAGE_MAX_LENGTH"
        :placeholder="placeholder"
        :object-id="metadataObjectId"
        :tag-enabled="false"
        ck-editor-type="taskContent"
        object-type="task"
        autofocus
        content-link-enabled
        disable-suggester
        @ready="onEditorReady" />
    </div>
    <div
      v-if="task.id && displayEditor && editorReady"
      class="d-flex justify-end mt-2">
      <v-btn
        id="cancelDescriptionButton"
        class="btn me-2"
        @click="cancelDescription($event)">
        {{ $t('popup.cancel') }}
      </v-btn>
      <v-btn
        id="saveDescriptionButton"
        :loading="savingDescription"
        :disabled="saveDescriptionButtonDisabled"
        depressed
        outlined
        class="btn px-2 btn-primary v-btn v-btn--contained theme--light v-size--default"
        @click="saveDescription($event)">
        {{ $t('label.apply') }}
      </v-btn>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: String,
      default: ''
    },
    readOnly: {
      type: Boolean,
      default: false,
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
      description: null,
      savingDescription: false,
      previousDescription: null,
      expanded: false,
      clamped: false,
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
    value: {
      immediate: true,
      handler() {
        if (this.value !== this.description) {
          this.description = this.value;
        }
        // Measured once the new text is rendered
        this.$nextTick(this.measureClamp);
      },
    },
    description() {
      if (this.value !== this.description) {
        this.$emit('input', this.description);
      }
    },
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
  mounted() {
    // The text's width changes with the drawer's, side panel or full screen, not only with the window's
    if (window.ResizeObserver && this.$refs.descriptionText) {
      this.descriptionObserver = new ResizeObserver(() => this.measureClamp());
      this.descriptionObserver.observe(this.$refs.descriptionText);
    }
  },
  beforeDestroy() {
    this.descriptionObserver?.disconnect();
  },
  created() {
    this.$root.$off('drawerClosed');
    this.$root.$on('drawerClosed', () => {
      this.hideDescriptionEditor();
    });
    document.addEventListener('onAddTask', () => {
      this.$emit('input', this.description);
      this.showEditor = false;
    });
    this.inputVal = this.value || '';
    $('#task-Drawer').on('click', (event) => {
      if (this.showEditor && event?.target && !$(event.target).parents('#taskDescriptionId').length) {
        this.$emit('input', this.description);
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
      if (!this.showEditor) {
        this.previousDescription = this.task?.description || '';
        this.inputVal = this.value;
        this.editorReady = true;
        this.showEditor = true;
      }
    },

    /**
     * Opens a link of the description in a new tab. A click on the text
     * itself does nothing: its pencil opens the editor, and an empty
     * description shows a button inviting to write one.
     *
     * @param {MouseEvent} event the click
     * @returns {void}
     */
    onDescriptionClick(event) {
      const link = event?.target?.closest?.('a');
      if (link) {
        event.preventDefault();
        event.stopPropagation();
        window.open(link.href, '_blank');
      }
    },

    /**
     * Closes the editor and gives the description back the text it had when
     * the editor opened, as stored, not as the editor emits it.
     *
     * @param {Event} [event] the click on Cancel
     * @returns {void}
     */
    cancelDescription(event) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      this.description = this.previousDescription;
      this.$emit('input', this.previousDescription);
      this.hideDescriptionEditor();
    },

    /**
     * Lets Escape cancel the edit inside the editor, without the key reaching
     * the drawer, which would close.
     *
     * @returns {void}
     */
    onEditorReady() {
      const editor = this.$refs.taskDescriptionEditor?.editor;
      if (editor) {
        editor.on('key', evt => {
          if (evt.data.keyCode === 27) {
            evt.cancel();
            evt.data.domEvent?.$?.stopPropagation?.();
            this.cancelDescription();
          }
        });
      }
    },

    /**
     * Tells whether the description is longer than its five shown lines, so
     * that it offers « See more ».
     *
     * @returns {void}
     */
    measureClamp() {
      const text = this.$refs.descriptionText;
      if (text && !this.expanded) {
        this.clamped = text.scrollHeight > text.clientHeight + 1;
      }
    },
  }
};
</script>
