package com.example

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class InstalledApp(
    val id: String,
    val name: String,
    val subtitle: String,
    val iconName: String,
    val badge: String? = null,
    val accentColorHex: Long = 0xFF4285F4,
    val isDefault: Boolean = false,
    val webUrl: String? = null,
    val installedAt: Long = System.currentTimeMillis()
)

class AppManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val _catalog = MutableStateFlow<List<AppCatalogItem>>(DefaultAppCatalog.catalog)
    val catalog: StateFlow<List<AppCatalogItem>> = _catalog.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(
        DefaultAppCatalog.catalog
            .filter { it.isInstalledByDefault }
            .map { item ->
                InstalledApp(
                    id = item.id,
                    name = item.name,
                    subtitle = item.subtitle,
                    iconName = item.iconName,
                    badge = item.badge,
                    accentColorHex = item.accentColorHex,
                    isDefault = item.isInstalledByDefault,
                    webUrl = item.webUrl
                )
            }
    )
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AppCategory.ALL)
    val selectedCategory: StateFlow<AppCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _appStoreNotice = MutableStateFlow<String?>(null)
    val appStoreNotice: StateFlow<String?> = _appStoreNotice.asStateFlow()

    fun setCategory(category: AppCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearNotice() {
        _aideNotice()
    }

    private fun _aideNotice() {
        _appStoreNotice.value = null
    }

    fun isAppInstalled(appId: String): Boolean {
        return _installedApps.value.any { it.id == appId }
    }

    fun installApp(catalogItem: AppCatalogItem) {
        if (isAppInstalled(catalogItem.id)) {
            _appStoreNotice.value = "${catalogItem.name} is already installed on Home Screen."
            return
        }

        val newApp = InstalledApp(
            id = catalogItem.id,
            name = catalogItem.name,
            subtitle = catalogItem.subtitle,
            iconName = catalogItem.iconName,
            badge = catalogItem.badge,
            accentColorHex = catalogItem.accentColorHex,
            isDefault = false,
            webUrl = catalogItem.webUrl
        )

        _installedApps.value = _installedApps.value + newApp
        _appStoreNotice.value = "Installed \"${catalogItem.name}\" to Home Screen!"
        Toast.makeText(context, "Added ${catalogItem.name} to Home Screen", Toast.LENGTH_SHORT).show()
    }

    fun uninstallApp(appId: String) {
        val app = _installedApps.value.firstOrNull { it.id == appId }
        if (app != null) {
            _installedApps.value = _installedApps.value.filterNot { it.id == appId }
            _appStoreNotice.value = "Removed \"${app.name}\" from Home Screen."
            Toast.makeText(context, "Removed ${app.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun installCustomWebApp(name: String, url: String, iconKey: String = "web") {
        if (name.isBlank() || url.isBlank()) {
            _appStoreNotice.value = "Please enter both a name and a valid URL."
            return
        }

        val formattedUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> "https://$url"
        }

        val newId = "custom_app_${UUID.randomUUID().toString().take(8)}"
        val customApp = InstalledApp(
            id = newId,
            name = name.trim(),
            subtitle = "Custom Web App",
            iconName = iconKey,
            badge = "WEB",
            accentColorHex = 0xFF00BCD4,
            isDefault = false,
            webUrl = formattedUrl
        )

        _installedApps.value = _installedApps.value + customApp
        _appStoreNotice.value = "Pinned \"${customApp.name}\" to Home Screen!"
        Toast.makeText(context, "Installed ${customApp.name} to Home Screen", Toast.LENGTH_SHORT).show()
    }
}
