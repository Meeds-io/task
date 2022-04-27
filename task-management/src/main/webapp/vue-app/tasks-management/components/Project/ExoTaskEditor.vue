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
    :class="charsCount > 0 ? '' : 'emptyEditor'"
    class="projectDescription" 
    style="position: relative">
    <textarea
      ref="editor"
      :id="descriptionTaskContent"
      v-model="inputVal"
      :placeholder="placeholder"
      cols="30"
      rows="10"
      class="textarea"></textarea>
    <div 
      v-show="editorReady" 
      :class="charsCount > maxLength ? 'tooManyChars' : ''" 
      class="activityCharsCount" 
      style="">
      {{ charsCount }}{{ maxLength > -1 ? ' / ' + maxLength : '' }}
      <i class="uiIconMessageLength"></i>
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
    reset: {
      type: Boolean,
      default: false
    },
    placeholder: {
      type: String,
      default: ''
    },
    id: {
      type: String,
      default: ''
    },
    maxLength: {
      type: Number,
      default: -1
    },
  },
  data() {
    return {
      SMARTPHONE_LANDSCAPE_WIDTH: 768,
      descriptionTaskContent: '',
      inputVal: this.value,
      editorReady: false,
    };
  },
  computed: {
    charsCount() {
      const pureText = this.$utils.htmlToText(this.inputVal);
      return pureText.length;
    },
  },
  watch: {
    inputVal(val) {
      this.$emit('input', val);
    },
    value(val) {
      for (const instances in CKEDITOR.instances){
        if (CKEDITOR.instances[instances].element.$.id === this.descriptionTaskContent) {
          const editorData = CKEDITOR.instances[instances].getData();
          // watch value to reset the editor value if the value has been updated by the component parent
          if (editorData != null && val !== editorData) {
            if (val === '') {
              this.initCKEditor(instances);
            } else {
              CKEDITOR.instances[instances].setData(val);
              CKEDITOR.instances[instances].setData(val);
            }
          }
        }
      }
    },
    reset() {
      CKEDITOR.instances[this.descriptionTaskContent].destroy(true);
      this.initCKEditor();
    },
  },
  mounted() {
    if (this.id !== '' || this.id !== null){
      this.descriptionTaskContent = 'descriptionTaskContent'.concat(this.id);
    } else {
      this.descriptionTaskContent = 'descriptionTaskContent';
    }
    this.initCKEditor();
  },
  methods: {
    initCKEditor: function (instances) {
      CKEDITOR.plugins.addExternal('/commons-extension/eXoPlugins/embedsemantic/','plugin.js');
      if (typeof CKEDITOR.instances[instances] !== 'undefined') {
        CKEDITOR.instances[instances].destroy(true);
      }
      let extraPlugins = 'simpleLink,widget';
      const windowWidth = $(window).width();
      const windowHeight = $(window).height();
      if (windowWidth > windowHeight && windowWidth < this.SMARTPHONE_LANDSCAPE_WIDTH) {
        // Disable suggester on smart-phone landscape
        extraPlugins = 'simpleLink';
      }
      // this line is mandatory when a custom skin is defined
      CKEDITOR.basePath = '/commons-extension/ckeditor/';
      const self = this;
      $(this.$refs.editor).ckeditor({
        customConfig: '/commons-extension/ckeditorCustom/config.js',
        extraPlugins: extraPlugins,
        allowedContent: true,
        removePlugins: 'image,confirmBeforeReload,maximize,resize',
        toolbar: [
          ['Bold', 'Italic', 'BulletedList', 'NumberedList', 'Blockquote'],
        ],
        typeOfRelation: 'mention_activity_stream',
        autoGrow_onStartup: true,
        //autoGrow_maxHeight: 300,
        on: {
          instanceReady: function() {
            self.editorReady = true;
          },
          change: function(evt) {
            const newData = evt.editor.getData();
            self.inputVal = newData;
          },
          destroy: function () {
            self.inputVal = '';
          }
        }
      });
    },
    setFocus: function() {
      CKEDITOR.instances[this.descriptionTaskContent].focus();
    },
    getMessage: function() {
      const newData = CKEDITOR.instances[this.descriptionTaskContent].getData();
      return newData ? newData : '';
    },
  }
};
</script>
