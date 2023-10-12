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
  <div v-if="editorReady">
    <div class="comment d-flex align-start mb-2">
      <exo-user-avatar
        :profile-id="currentUserName"
        :size="30"
        :url="null"
        extra-class="me-2" 
        avatar />
      <rich-editor
        ref="taskCommentEditor"
        v-model="inputVal"
        :ck-editor-id="id"
        :max-length="MESSAGE_MAX_LENGTH"
        :placeholder="placeholder"
        :object-id="newCommentId"
        :tag-enabled="false"
        :suggester-space-url="getSpaceUrlFromProjectParticipator()"
        ck-editor-type="taskCommentContent"
        object-type="taskComment"
        autofocus
        class="editorContainer"
        @attachments-edited="attachmentsEdit"
        @validity-updated="validMessage = $event" />
    </div>
    <v-btn
      :disabled="postDisabled"
      depressed
      small
      type="button" 
      class="btn btn-primary ignore-vuetify-classes btnStyle ms-10 mb-2 commentBtn"
      @click="addNewComment()">
      {{ $t('comment.label.comment') }}
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
      inputVal: null,
      charsCount: 0,
      disabledComment: '',
      MESSAGE_MAX_LENGTH: 1300,
      currentUserName: eXo.env.portal.userName,
      currentCommentId: '',
      newCommentId: null,
      editorReady: false,
      validMessage: true,
      taskCommentAttachmentsEdited: false
    };
  },
  watch: {
    inputVal(val) {
      this.$emit('input', val);
      this.disabledComment = val === '';
    },
    editorReady ( val ) {
      if ( val === true ) {
        if (this.$refs.taskCommentEditor) {
          this.$refs.taskCommentEditor.initCKEditor();
        }
      } else {
        if (this.$refs.taskCommentEditor) {
          this.$refs.taskCommentEditor.destroyCKEditor();
        }
      }
    }
  },
  computed: {
    postDisabled() {
      return !this.validMessage || (!this.inputVal && !this.taskCommentAttachmentsEdited);
    },
  },
  mounted() {
    this.$root.$on('task-comment-created', () => {
      this.reset();
    });

    document.addEventListener('Task-comments-drawer-closed', this.reset);

    this.$root.$on('showEditor', commentId => {
      this.$nextTick().then(() => {
        this.showEditor(commentId);
      });
    });
    
    if ( this.showCommentEditor ) {
      this.editorReady = true;
    }
    this.$root.$on('newCommentEditor', (lastComment) => {
      this.editorReady = false;
      this.newCommentId = null;
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
  beforeDestroy() {
    document.removeEventListener('Task-comments-drawer-closed', this.reset);
  },
  methods: {
    showEditor(commentId) {
      this.currentCommentId = commentId;
      this.newCommentId = null;
      this.editorReady =  `commentContent-${commentId}`  === this.id;
    },
    getMessage() {
      return this.inputVal;
    },
    addNewComment() {
      this.$emit('addNewComment', this.commentId);
    },
    reset() {
      this.inputVal = '';
      this.editorReady = false;
      this.taskCommentAttachmentsEdited = false;
    },
    saveAttachments(commentId) {
      this.newCommentId = commentId;
      return this.$nextTick()
        .then(() => this.$refs.taskCommentEditor.saveAttachments());
    },
    attachmentsEdit(attachments) {
      this.$emit('attachments-edited');
      if (attachments?.length) {
        this.taskCommentAttachmentsEdited = true;
      }
    },
    getSpaceUrlFromProjectParticipator() {
      const participator = this.task.status.project.participator;
      if (participator && participator.length > 0) {
        const projectParticipator = participator.find((element) => element.startsWith('member:/spaces/'));
        if (projectParticipator) {
          return projectParticipator.substring(15);
        }
      }
    },
  },
};
</script>
