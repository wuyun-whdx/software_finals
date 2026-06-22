<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { fetchProvinces, fetchCities, fetchDistricts, getMyRegion, saveMyRegion } from '../services/regionService'
import type { RegionInfo, SimpleRegion } from '../types'

const emit = defineEmits<{
  (e: 'update:hasRegion', value: boolean): void
  (e: 'regionChanged'): void
}>()

const provinces = ref<SimpleRegion[]>([])
const cities = ref<SimpleRegion[]>([])
const districts = ref<SimpleRegion[]>([])

const selectedProvince = ref<SimpleRegion | null>(null)
const selectedCity = ref<SimpleRegion | null>(null)
const selectedDistrict = ref<SimpleRegion | null>(null)

const region = ref<RegionInfo | null>(null)
const saving = ref(false)
const loaded = ref(false)
const error = ref('')

async function loadProvinces() {
  try {
    provinces.value = await fetchProvinces()
  } catch {
    error.value = '加载省份数据失败'
  }
}

async function loadRegion() {
  try {
    const r = await getMyRegion()
    region.value = r
    emit('update:hasRegion', !!r)
  } catch {
    // No region yet
  }
}

onMounted(async () => {
  await loadProvinces()
  await loadRegion()
  loaded.value = true
})

watch(selectedProvince, async (p) => {
  selectedCity.value = null
  selectedDistrict.value = null
  cities.value = []
  districts.value = []
  if (p) {
    try { cities.value = await fetchCities(p.id) } catch { error.value = '加载城市数据失败' }
  }
})

watch(selectedCity, async (c) => {
  selectedDistrict.value = null
  districts.value = []
  if (c) {
    try { districts.value = await fetchDistricts(c.id) } catch { error.value = '加载区县数据失败' }
  }
})

async function save() {
  if (!selectedProvince.value || !selectedCity.value) return
  saving.value = true
  error.value = ''
  try {
    const payload: RegionInfo = {
      province: selectedProvince.value.name,
      city: selectedCity.value.name,
      district: selectedDistrict.value?.name
    }
    const saved = await saveMyRegion(payload)
    region.value = saved
    emit('update:hasRegion', true)
    emit('regionChanged')
  } catch (e) {
    error.value = (e as Error).message || '保存地域失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="region-selector">
    <div v-if="!loaded" class="region-loading">加载地域数据...</div>
    <template v-else>
      <div class="region-fields">
        <select v-model="selectedProvince" class="field select">
          <option :value="null" disabled>选择省份</option>
          <option v-for="p in provinces" :key="p.id" :value="p">{{ p.name }}</option>
        </select>
        <select v-model="selectedCity" class="field select" :disabled="!selectedProvince">
          <option :value="null" disabled>选择城市</option>
          <option v-for="c in cities" :key="c.id" :value="c">{{ c.name }}</option>
        </select>
        <select v-model="selectedDistrict" class="field select" :disabled="!selectedCity">
          <option :value="null">不限区县</option>
          <option v-for="d in districts" :key="d.id" :value="d">{{ d.name }}</option>
        </select>
        <button class="primary" type="button" :disabled="!selectedProvince || !selectedCity || saving" @click="save">
          {{ saving ? '保存中...' : '确认' }}
        </button>
      </div>
      <div v-if="region" class="region-current">
        当前地域：{{ region.province }} {{ region.city }}{{ region.district ? ' ' + region.district : '' }}
      </div>
      <div v-else class="region-hint">填写所在地，获取 AI 精准推荐</div>
      <div v-if="error" class="region-error">{{ error }}</div>
    </template>
  </div>
</template>

<style scoped>
.region-selector {
  margin-bottom: 1.5rem;
}
.region-fields {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}
.region-fields select {
  min-width: 120px;
}
.region-current {
  margin-top: 0.5rem;
  font-size: 0.875rem;
  color: var(--muted);
}
.region-hint {
  margin-top: 0.5rem;
  font-size: 0.875rem;
  color: var(--muted);
  font-style: italic;
}
.region-error {
  margin-top: 0.5rem;
  color: var(--error, #e53e3e);
  font-size: 0.875rem;
}
.region-loading {
  font-size: 0.875rem;
  color: var(--muted);
}
</style>
