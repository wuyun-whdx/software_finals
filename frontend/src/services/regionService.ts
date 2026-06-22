import { regionApi } from '../api'
import type { RegionInfo, RegionRecord, SimpleRegion } from '../types'

export async function fetchProvinces() {
  return regionApi.provinces() as Promise<SimpleRegion[]>
}

export async function fetchCities(provinceId: number) {
  return regionApi.cities(provinceId) as Promise<SimpleRegion[]>
}

export async function fetchDistricts(cityId: number) {
  return regionApi.districts(cityId) as Promise<SimpleRegion[]>
}

export async function getMyRegion() {
  return regionApi.getMyRegion() as Promise<RegionInfo | null>
}

export async function saveMyRegion(region: RegionInfo) {
  return regionApi.saveMyRegion(region) as Promise<RegionInfo>
}

export async function regionHistory() {
  return regionApi.history() as Promise<RegionRecord[]>
}
