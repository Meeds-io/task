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
  <div
    id="taskDescriptionId"
    :class="editorReady && 'active'"
    class="taskDescription">
    <div
      :data-text="placeholder"
      :title="$t('tooltip.clickToEdit')"
      contentEditable="true"
      class="py-1 px-2 taskDescriptionToShow"
      @click="showDescriptionEditor($event)"
      v-sanitized-html="value">
      {{ placeholder }}
    </div>
    <exo-task-editor
      v-if="displayEditor"
      ref="richEditor"
      v-model="inputVal"
      :max-length="MESSAGE_MAX_LENGTH"
      :id="task.id"
      :placeholder="$t('task.placeholder').replace('{0}', MESSAGE_MAX_LENGTH)" />
    <v-btn
      v-if="task.id && displayEditor"
      id="saveDescriptionButton"
      :loading="savingDescription"
      :disabled="saveDescriptionButtonDisabled"
      depressed
      outlined
      class="btn mt-1 ml-auto d-flex px-2 btn-primary v-btn v-btn--contained theme--light v-size--default"
      @click="saveDescription">
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
    reset: {
      type: Boolean,
      default: false
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
      savingDescription: false,
    };
  },
  computed: {
    saveDescriptionButtonDisabled() {
      return this.savingDescription || this.inputVal?.length > this.MESSAGE_MAX_LENGTH;
    },
    taskDescription() {
      return this.task?.description || '';
    },
    displayEditor() {
      return this.showEditor;
    },
  },
  watch: {
    inputVal(val) {
      const editorData = CKEDITOR.instances['descriptionContent'] && CKEDITOR.instances['descriptionContent'].getData();
      if (editorData != null && val !== editorData) {
        if (val === '') {
          CKEDITOR.instances['descriptionContent'].setData('');
          this.initCKEditor();
        } else {
          CKEDITOR.instances['descriptionContent'].setData(val);
        }
      }
      if (this.editorReady) {
        this.$emit('input', val);
      }
    },
    editorReady(val) {
      const ckeContent = document.querySelectorAll('[id=cke_descriptionContent]');
      if (val === true) {
        this.initCKEditor();
        if (ckeContent) {
          for (let i = 0; i < ckeContent.length; i++) {
            ckeContent[i].classList.remove('hiddenEditor');
          }
        }
        document.getElementById('taskDescriptionId').classList.remove('taskDescription');
        if (CKEDITOR.instances['descriptionContent']) {
          CKEDITOR.instances['descriptionContent'].focus(true);
        }
      } else {
        for (let i = 0; i < ckeContent.length; i++) {
          ckeContent[i].classList.add('hiddenEditor');
          document.getElementById('taskDescriptionId').classList.add('taskDescription');
        }
      }
    },
    reset() {
      CKEDITOR.instances['descriptionContent'].destroy(true);
      this.editorReady = false;
    },
  },
  created() {
    this.$root.$off('drawerClosed');
    this.$root.$on('drawerClosed', () => {
      this.editorReady = false;
    });
    document.addEventListener('onAddTask', () => {
      this.$emit('addTaskDescription',this.inputVal);
      this.editorReady = false;
    });
    this.inputVal = this.value || '';
  },
  methods: {
    saveDescription() {
      const newValue = this.inputVal?.replace('&nbsp;',' ');
      if (this.task.id && !isNaN(this.task.id)) {
        this.savingDescription = true;

        const task = JSON.parse(JSON.stringify(this.task));
        task.description = newValue;
        this.$taskDrawerApi.updateTask(this.task.id , task)
          .then(() => {
            this.task.description = newValue;
            this.$root.$emit('show-alert', {
              type: 'success',
              message: this.$t('alert.success.task.description')
            });
            this.$root.$emit('task-description-updated', this.task);
            this.editorReady = false;
            this.showEditor = false;
          }).catch(() => {
            this.$root.$emit('show-alert',{
              type: 'error',
              message: this.$t('alert.error')
            });
          }).finally(() => this.savingDescription = false);
      }
    },
    initCKEditor: function () {
      CKEDITOR.plugins.addExternal('embedsemantic', '/commons-extension/eXoPlugins/embedsemantic/', 'plugin.js');
      let extraPlugins = 'simpleLink,widget,embedsemantic';
      const windowWidth = $(window).width();
      const windowHeight = $(window).height();
      if (windowWidth > windowHeight && windowWidth < 768) {
        extraPlugins = 'simpleLink,selectImage,embedsemantic';
      }
      CKEDITOR.basePath = '/commons-extension/ckeditor/';
      const self = this;
      $(this.$refs.editor).ckeditor({
        customConfig: '/commons-extension/ckeditorCustom/config.js',
        extraPlugins: extraPlugins,
        removePlugins: 'confirmBeforeReload,maximize,resize',
        toolbarLocation: 'bottom',
        startupFocus: self.inputVal=== '' ? true :'end',
        autoGrow_onStartup: true,
        on: {
          blur: function () {
            $(document.body).trigger('click');
            self.editorReady = false;
          },
          change: function(evt) {
            const newData = evt.editor.getData();
            self.inputVal = newData;
          },
          destroy: function () {
            self.editorReady = false;
            self.inputVal = '';
          }
        },
      });
    },
    hideDescriptionEditor() {
      this.editorReady = false;
      this.showEditor = false;
    },
    showDescriptionEditor: function (event) {
      if (!this.showEditor && event?.target?.nodeName !== 'A') {
        this.inputVal = this.value;
        this.editorReady = true;
        this.showEditor = true;
      } else if (event?.target?.nodeName === 'A') {
        window.open(event.target.href, '_blank');
      }
    },
    urlVerify(text) {
      return this.$taskDrawerApi.urlVerify(text);
    }

  }
};
</script>
