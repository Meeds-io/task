/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
 
export function getMyAllTasks(itemsLimit) {
  return fetch(`/portal/rest/tasks?limit=${itemsLimit}`, {
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } else {
      throw new Error ('Error when getting my all tasks');
    }
  });
}

export function getMyIncomingTasks(itemsLimit) {
  return fetch(`/portal/rest/tasks?status=incoming&limit=${itemsLimit}&returnSize=true`, {
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } 
    else {
      throw new Error ('Error when getting my incoming tasks');
    }
  });
}

export function getMyOverdueTasks(itemsLimit) {
  return fetch(`/portal/rest/tasks?status=overdue&limit=${itemsLimit}&returnSize=true`, {
    method: 'GET',
  }).then((resp) => {
    if (resp && resp.ok) {
      return resp.json();
    } 
    else {
      throw new Error ('Error when getting my overdue tasks');
    }
  });
}