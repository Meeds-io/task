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
  <v-card 
    class="tasksCardItem" 
    flat
    outlined
    hover>
    <div class="projectDetailsReverse pa-3">
      <i
        icon
        small
        class="uiIconInformation taskInfoIcon reverseInfoIcon d-flex"
        @click="$emit('flip')">
      </i>
      <div class="projectDetails d-flex">
        <p class="font-weight-bold ma-auto ">{{ $t('label.projectDetail') }}</p>
      </div>
    </div>
    <div 
      v-if=" totalLeftTasks > 0 "
      class="echartStatsContent d-flex justify-space-evently align-center px-2"
      style="margin:auto;">
      <div class="echartAndLabel">
        <div 
          :id="'echartProjectTasks'+project.id" 
          class="echartPieContent" 
          style="width:160px; height:200px;"></div>
        <div class="projectTasksTotalNumber">
          <span class="totalNumber font-weight-bold">{{ totalLeftTasks }}</span>
          <span class="text-body-2 totalLabel">{{ $t('exo.tasks.label.leftTasks') }}</span>
        </div>
      </div>
      <div v-if="statistics.length < maxStatusToShow" class="projectStatusNumber ps-4">
        <p 
          v-for="item in statistics" 
          :key="item.name" 
          class="d-flex justify-space-between mb-1 taskToDoLabel">
          <span class="caption text-truncate">{{ item.name }}</span>
          <span>{{ item.value }}</span>
        </p>
      </div>
    </div>
    <div v-else class="noTasksProject">
      <div class="noTasksProjectIcon"><i class="uiIcon uiIconTask"></i></div>
      <div class="noTasksProjectLabel"><span>{{ $t('label.noTasks') }}</span></div>
      <!-- <div class="noTasksProjectLink"><a href="#">{{ $t('label.addTask') }}</a></div> -->
    </div>
  </v-card>
</template>
<script>
export default {
  props: {
    project: {
      type: Object,
      default: null,
    }
  },
  data() {
    return {
      totalLeftTasks: 0,
      statistics: [],
      maxStatusToShow: 7,
      option: {
        tooltip: {
          trigger: 'item',
          formatter: '{b}:<br/> {c} ({d}%)'
        },
        series: [
          {
            type: 'pie',
            center: ['50%', '50%'],
            radius: ['80%', '75%'],
            avoidLabelOverlap: false,
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: false,
                fontSize: '15',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: [],
          }
        ],
        color: ['#476a9c', '#ffb441', '#bc4343', '#2eb58c']
      }
    };
  },

  methods: {
    initChart(option) {
      const holder_chart = $(`#echartProjectTasks${this.project.id}`)[0];
      if (holder_chart){
        const chart = echarts.init(holder_chart);
        chart.setOption(option, true);}
    },
    getStats(project){
      this.$projectService.getProjectStats(project.id).then(data => {
        this.statistics = data.statusStats || [];
        this.totalLeftTasks = data.totalNumberTasks || 0;

        if (this.statistics && this.statistics.length) {
          this.option.series[0].data=this.statistics;
          window.setTimeout(() => {
            this.initChart(this.option);
          },200);
        }
      });
    }
  }
};
</script>