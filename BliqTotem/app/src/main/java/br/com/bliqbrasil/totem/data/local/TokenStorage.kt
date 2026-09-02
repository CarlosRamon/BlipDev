package br.com.bliqbrasil.totem.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_FILE = "bliq_secure_prefs"
private const val KEY_TOKEN  = "@bliq:pos_token"
private const val KEY_BOX_TIPO = "@bliq:box_tipo"
private const val KEY_POLITICA_CPF = "@bliq:politica_cpf"

class TokenStorage(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    // Fonte da verdade do tipo de box. Fica aqui — e não na Application — porque
    // todo ViewModel que carrega /config já recebe o TokenStorage e pode publicar
    // o tipo assim que souber dele, sem depender de qual tela carregou primeiro.
    private val _boxTipo = MutableStateFlow(prefs.getString(KEY_BOX_TIPO, null) ?: DEFAULT_BOX_TIPO)

    val boxTipo: StateFlow<String> = _boxTipo.asStateFlow()

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_BOX_TIPO).remove(KEY_POLITICA_CPF).apply()
        _boxTipo.value = DEFAULT_BOX_TIPO
    }

    // Tipo do box (LAVACAO/ASPIRACAO). Persistido para que o tema já abra na cor
    // correta no boot, antes de /config responder — senão o totem pisca azul→navy.
    // Política de identificação da franquia. Persistida junto do tipo do box
    // para que a decisão de pular a tela de CPF já valha no primeiro frame.
    fun savePoliticaCpf(politica: String) =
        prefs.edit().putString(KEY_POLITICA_CPF, politica).apply()

    fun getPoliticaCpf(): String? = prefs.getString(KEY_POLITICA_CPF, null)

    fun saveBoxTipo(tipo: String) {
        prefs.edit().putString(KEY_BOX_TIPO, tipo).apply()
        _boxTipo.value = tipo
    }

    private companion object {
        const val DEFAULT_BOX_TIPO = "LAVACAO"
    }
}
