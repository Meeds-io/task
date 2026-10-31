<!--
  This file is part of the Meeds project (https://meeds.io/).
  Copyright (C) 2026 Meeds Association
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
  <v-list-item class="pe-0">
    <v-list-item-content class="mt-n1">
      <div class="d-flex">
        <exo-user-avatar
          :profile-id="item.author"
          :size="30"
          :url="false"
          popover
          align-top />
        <div class="d-flex" v-sanitized-html="renderChangeHTML(item)"></div>
      </div>
      <div>
        <div class="dateTime caption px-10 my-n3">
          <date-format :value="item.createdTime" :format="dateTimeFormat" />
        </div>
      </div>
    </v-list-item-content>
  </v-list-item>
</template>
<script>
export default {
  props: {
    item: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    dateTimeFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    },
  }),
  methods: {
    /**
     * @param {object} item a change of the task
     * @returns {string} the key of the label naming its action
     */
    logMsg(item) {
      return `log.${ item.actionName }`;
    },
    /**
     * @param {object} item a change of the task
     * @returns {string} the HTML line saying what changed: the action, and its
     *          target where it has one, a person's profile link for an
     *          assignment
     */
    renderChangeHTML(item) {
      let str = '';
      if (item.actionName === 'assign' || item.actionName === 'unassign' || item.actionName === 'assignCoworker' || item.actionName === 'unassignCoworker') {
        const targetFullName = item.isTargetFullNameExternal ? `${item.targetFullName} (${this.$t('label.external')})` :  `${item.targetFullName}`;
        str = `<p class='text-truncate my-auto text-color ms-1 subtitle-2' title='${item.authorFullName} ${this.$t(this.logMsg(item))} ${targetFullName}'>` +
            `<span>${ this.$t(this.logMsg(item))}</span>`+
            `<a href='/portal/dw/profile/${item.target}'> ${targetFullName} </a>`+
            '</p>';
      } else if ( item.actionName === 'edit_project' ) {
        str = `<p class='text-truncate my-auto text-color ms-1 subtitle-2' title='${item.authorFullName} ${this.$t(this.logMsg(item))}  ${item.target}'>` +
            `<span>${ this.$t(this.logMsg(item))}</span>`+
            `<a href='#'> ${item.target} </a>`+
            '</p>';
      } else if ( item.actionName === 'created' ) {
        // the target of a creation entry is the status the task was created in, not shown
        str = `<p class='text-truncate my-auto text-color ms-1 subtitle-2' title='${item.authorFullName} ${this.$t(this.logMsg(item))}'> ${ this.$t(this.logMsg(item)) }</p>`;
      } else if ( item.actionName === 'edit_priority' ) {
        str = `<p class='text-truncate my-auto text-color ms-1 subtitle-2' title='${item.authorFullName} ${this.$t(this.logMsg(item))} ${item.task.priority}'> ${ this.$t(this.logMsg(item)) } ${item.task.priority}</p>`;
      } else {
        str = `<p class='text-truncate my-auto text-color ms-1 subtitle-2' title='${item.authorFullName} ${this.$t(this.logMsg(item))} ${item.target}'> ${ this.$t(this.logMsg(item)) } ${item.target}</p>`;
      }
      return str;
    },
  },
};
</script>
