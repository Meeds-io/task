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
  <div class="comment d-flex align-start" :class="editorReady && 'activeEditor' || 'inactiveEditor'">
    <exo-user-avatar
      :profile-id="currentUserName"
      :size="30"
      :url="null"
      extra-class="me-2" 
      avatar />
    <div class="editorContainer">
      <textarea 
        :ref="`editor-${id}`"
        :id="id" 
        v-model="inputVal" 
        :placeholder="placeholder" 
        cols="30" 
        rows="10" 
        class="textarea"
        autofocus></textarea>
      <div
        :class="charsCount > maxLength ? 'tooManyChars' : ''"
        class="activityCharsCount"
        style="">
        {{ charsCount }}{{ maxLength > -1 ? ' / ' + maxLength : '' }}
        <i class="uiIconMessageLength"></i>
      </div>
      <v-btn
        :disabled="postDisabled"
        depressed
        small
        type="button" 
        class="btn btn-primary ignore-vuetify-classes btnStyle mt-1 mb-2 commentBtn"
        @click="addNewComment()">
        {{ $t('comment.label.comment') }}
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
    task: {
      type: Object,
      default: null
    },
    placeholder: {
      type: String,
      default: ''
    },
    maxLength: {
      type: Number,
      default: -1
    },
    id: {
      type: String,
      default: ''
    },
    editorReady: {
      type: Boolean,
      default: false
    },
    showCommentEditor: {
      type: Boolean,
      default: false
    },
    lastComment: {
      type: String,
      default: ''
    },
    commentId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      inputVal: this.value,
      charsCount: 0,
      disabledComment: '',
      MESSAGE_MAX_LENGTH: 1250,
      currentUserName: eXo.env.portal.userName,
      currentCommentId: ''
    };
  },
  computed: {
    postDisabled() {
      if (this.disabledComment) {
        return true;
      } else if (this.inputVal !== null && this.inputVal!=='') {
        let pureText = this.inputVal ? this.inputVal.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim() : '';
        const div = document.createElement('div');
        div.innerHTML = pureText;
        pureText = div.textContent || div.innerText || '';
        return pureText.length > this.MESSAGE_MAX_LENGTH;
      } else {return true;}
    },
  },
  watch: {
    inputVal(val) {
      this.$emit('input', val);
      this.disabledComment = val === '';
    },
    editorReady ( val ) {
      if ( val === true ) {
        this.initCKEditor();
      } else {
        if (CKEDITOR.instances[this.id]) {
          CKEDITOR.instances[this.id].destroy(true);
        }
      }
    }
  },
  created() {
    this.$root.$on('showEditor', commentId => {
      this.$nextTick().then(() => {
        this.showEditor(commentId);
      });
    });
  },
  mounted() {
    if ( this.showCommentEditor ) {
      this.editorReady = true;
    }
    this.$root.$on('newCommentEditor', (lastComment) => {
      this.editorReady = false;
      window.setTimeout(() => {
        this.showCommentEditor = `commentContent-${lastComment}` === this.id;
        if (this.showCommentEditor) {
          this.editorReady = true;
        }
      }, 100);
    });
    const thiss = this;
    $('body').suggester('addProvider', 'task:people', function (query, callback) {
      const _this = this;
      const projectId = thiss.task.status ? thiss.task.status.project.id : null;
      thiss.$taskDrawerApi.findUsersToMention(projectId, query).then((data) => {
        const result = [];
        for (let i = 0; i < data.length; i++) {
          const d = data[i];
          const item = {
            uid: d.id.substr(1),
            name: d.name,
            avatar: d.avatar
          };
          result.push(item);
        }
        callback.call(_this, result);
      });
    });
  },
  methods: {
    initCKEditor() {
      let extraPlugins = 'suggester,widget,embedsemantic';
      const windowWidth = $(window).width();
      const windowHeight = $(window).height();
      if (windowWidth > windowHeight && windowWidth < 768) {
        // Disable suggester on smart-phone landscape
        extraPlugins = 'selectImage';
      }
      CKEDITOR.basePath = '/commons-extension/ckeditor/';
      const self = this;
      $(this.$refs[`editor-${this.id}`]).ckeditor({
        customConfig: '/commons-extension/ckeditorCustom/config.js',
        extraPlugins: extraPlugins,
        removePlugins: 'confirmBeforeReload,maximize,resize',

        autoGrow_onStartup: true,
        on: {
          instanceReady: function() {
            self.setFocus();
          },
          change: function(evt) {
            const newData = evt.editor.getData();
            self.inputVal = newData;
            let pureText = newData ? newData.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim() : '';
            const div = document.createElement('div');
            div.innerHTML = pureText;
            pureText = div.textContent || div.innerText || '';
            self.charsCount = pureText.length;
          },
          destroy: function () {
            self.inputVal = '';
            self.charsCount = 0;
            self.editorReady = false;
            self.showCommentEditor =false;
          },
        },
        suggester: {
          /* eslint-disable no-template-curly-in-string */
          renderMenuItem: '<li data-value="${uid}"><div class="avatarSmall" style="display: inline-block;"><img src="${avatar}"></div>${name} (${uid})</li>',
          renderItem: '<span class="exo-mention">${name}<a href="#" class="remove"><i class="uiIconClose uiIconLightGray"></i></a></span>',
          sourceProviders: ['task:people']
        },

      });
    },
    getMessage() {
      const newData = CKEDITOR.instances[this.id].getData();
      return newData ? newData.trim() : '';
    },
    setFocus() {
      CKEDITOR.instances[this.id].focus();
    },
    destroyEditor() {
      CKEDITOR.instances[this.id].destroy();
    },
    showEditor(commentId) {
      this.currentCommentId = commentId;
      this.editorReady =  `commentContent-${commentId}`  === this.id;
    },
    addNewComment() {
      this.$emit('addNewComment', this.commentId);
      this.destroyEditor();
    }
  },
};
</script>
