package com.example.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {

    val allScans: Flow<List<ScanItem>> = scanDao.getAllScans()
    val scansCount: Flow<Int> = scanDao.getScansCount()

    fun getScanById(id: Long): Flow<ScanItem?> = scanDao.getScanById(id)

    fun searchScans(query: String): Flow<List<ScanItem>> = scanDao.searchScans(query)

    suspend fun insert(item: ScanItem): Long = scanDao.insertScan(item)

    suspend fun delete(item: ScanItem) = scanDao.deleteScan(item)

    suspend fun deleteById(id: Long) = scanDao.deleteScanById(id)

    suspend fun clearAll() = scanDao.clearAll()

    suspend fun setFavorite(id: Long, isFavorite: Boolean) = scanDao.updateFavorite(id, isFavorite)
}
