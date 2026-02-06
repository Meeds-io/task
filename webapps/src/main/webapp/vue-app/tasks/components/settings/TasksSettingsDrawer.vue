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
  <exo-drawer
    id="tasksSettingsDrawer"
    ref="tasksSettingsDrawer"
    :right="!$vuetify.rtl"
    @closed="reset">
    <template #title>
      <div class="text-truncate text-header-title font-weight-bold text-color">
        {{ $t('tasks.settings.edit.drawer.title') }}
      </div>
    </template>
    <template #content>
      <v-form ref="form">
        <div class="pa-5">
          <div class="text-header">
            {{ $t('tasks.settings.edit.drawer.updateSeeMore.label') }}
          </div>
          <v-text-field
            v-model="seeAllUrl"
            type="text"
            class="mb-1 pt-1"
            outlined
            dense />
          <div class="d-flex mt-1 align-center justify-space-between">
            <label class="v-label text-color align-start">
              {{ $t('tasks.settings.edit.drawer.opensInSameTab') }}
            </label>
            <div class="align-end">
              <v-switch
                v-model="sameTab"
                color="primary"
                class="pa-0 my-auto"
                hide-details />
            </div>
          </div>
        </div>
      </v-form>
    </template>
    <template #footer>
      <div class="d-flex width-fit-content ms-auto">
        <v-btn
          class="me-5 btn"
          @click="reset">
          {{ $t('tasks.settings.cancel.label') }}
        </v-btn>
        <v-btn
          class="btn btn-primary"
          :disabled="!saveEnabled"
          :loading="isSaving"
          @click="save">
          {{ $t('tasks.settings.save.label') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data() {
    return {
      isSaving: false,
      seeAllUrl: `/${eXo.env.portal.containerName}/${eXo.env.portal.metaPortalName}/tasks`,
      sameTab: true,
    };
  },
  props: {
    settings: {
      type: Object,
      default: null
    }
  },
  computed: {
    saveEnabled() {
      return this.settings.sameTab !== this.sameTab || this.settings.seeAllUrl !== this.seeAllUrl;
    },
    saveSettingsUrl() {
      return this.settings?.saveSettingsUrl;
    },
  },
  methods: {
    open() {
      this.restoreSavedSettings();
      this.$refs.tasksSettingsDrawer.open();
    },
    close() {
      this.$refs.tasksSettingsDrawer.close();
    },
    save() {
      this.isSaving = true;
      const settings = {
        seeAllUrl: this.seeAllUrl,
        sameTab: this.sameTab
      };
      this.$tasksService.saveSettings(this.$root.settingsSaveUrl, settings).then(() => {
        this.$root.$emit('alert-message', this.$t('tasks.settings.save.success.message'), 'success');
        this.$emit('settings-updated', settings);
        this.close();
      }).catch(() => {
        this.$root.$emit('alert-message', this.$t('tasks.settings.save.error.message'), 'error');
      })
        .finally(() => {
          this.isSaving = false;
        });
    },
    reset() {
      this.restoreSavedSettings();
      this.close();
    },
    restoreSavedSettings() {
      this.seeAllUrl = this.settings.seeAllUrl;
      this.sameTab = this.settings.sameTab;
    }
  }
};
</script>