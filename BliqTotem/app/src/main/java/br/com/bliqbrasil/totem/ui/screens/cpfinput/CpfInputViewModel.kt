package br.com.bliqbrasil.totem.ui.screens.cpfinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.bliqbrasil.totem.data.model.Cliente
import br.com.bliqbrasil.totem.data.model.TermosAtual
import br.com.bliqbrasil.totem.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CpfStep { ENTERING_CPF, LOADING, FOUND, NOT_FOUND, REGISTERING }

class CpfInputViewModel(private val repository: PosRepository) : ViewModel() {

    data class UiState(
        val step: CpfStep = CpfStep.ENTERING_CPF,
        val cpf: String = "",
        val nome: String = "",
        val telefone: String = "",
        val clienteEncontrado: Cliente? = null,
        val error: String? = null,
        // Termos vindos do backend (versão vigente + texto exibido no modal)
        val termos: TermosAtual? = null,
        val termosLoading: Boolean = false,
        val termosError: String? = null,
        // Anexo II — consentimentos separados
        val aceitouTermos: Boolean = false,
        val aceitaMarketing: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        carregarTermos()
    }

    fun carregarTermos() {
        _state.update { it.copy(termosLoading = true, termosError = null) }
        viewModelScope.launch {
            repository.getTermosAtual().fold(
                onSuccess = { t -> _state.update { it.copy(termos = t, termosLoading = false) } },
                onFailure = { _ ->
                    _state.update {
                        it.copy(
                            termosLoading = false,
                            termosError = "Não foi possível carregar os Termos de Uso. Tente novamente.",
                        )
                    }
                },
            )
        }
    }

    fun updateCpf(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(11)
        _state.update { it.copy(cpf = digits, error = null) }
        if (digits.length == 11 && validarCpf(digits)) buscarCliente(digits)
    }

    fun updateNome(value: String) = _state.update { it.copy(nome = value) }

    fun updateTelefone(value: String) {
        val digits = value.filter { it.isDigit() }.take(11)
        _state.update { it.copy(telefone = digits) }
    }

    fun toggleAceitouTermos(value: Boolean) =
        _state.update { it.copy(aceitouTermos = value) }

    fun toggleAceitaMarketing(value: Boolean) =
        _state.update { it.copy(aceitaMarketing = value) }

    fun cadastrar(onSuccess: (clienteId: String) -> Unit) {
        val s = _state.value
        if (!s.aceitouTermos) {
            _state.update { it.copy(error = "É necessário aceitar os Termos de Uso para criar o cadastro.") }
            return
        }
        val versao = s.termos?.versao
        if (versao == null) {
            _state.update { it.copy(error = "Termos ainda não carregados. Toque em \"Tentar novamente\".") }
            return
        }
        _state.update { it.copy(step = CpfStep.REGISTERING, error = null) }
        viewModelScope.launch {
            repository.criarCliente(
                cpf = s.cpf,
                nome = s.nome.trim(),
                telefone = s.telefone,
                termosVersao = versao,
                aceitaMarketing = s.aceitaMarketing,
            ).fold(
                onSuccess = { cliente -> onSuccess(cliente.id) },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            step = CpfStep.NOT_FOUND,
                            error = if (err.message?.contains("409") == true)
                                "CPF já cadastrado. Tente novamente."
                            else "Erro ao cadastrar. Tente novamente.",
                        )
                    }
                },
            )
        }
    }

    private fun buscarCliente(cpf: String) {
        _state.update { it.copy(step = CpfStep.LOADING, error = null) }
        viewModelScope.launch {
            repository.buscarCliente(cpf).fold(
                onSuccess = { resp ->
                    if (resp.encontrado && resp.cliente != null) {
                        _state.update { it.copy(step = CpfStep.FOUND, clienteEncontrado = resp.cliente) }
                    } else {
                        _state.update {
                            it.copy(
                                step = CpfStep.NOT_FOUND,
                                nome = resp.nomePreenchido?.let { n -> toTitleCase(n) } ?: "",
                            )
                        }
                    }
                },
                onFailure = {
                    _state.update { it.copy(step = CpfStep.NOT_FOUND, nome = "") }
                },
            )
        }
    }

    companion object {
        fun validarCpf(cpf: String): Boolean {
            if (cpf.length != 11 || cpf.all { it == cpf[0] }) return false
            fun digit(limit: Int): Int {
                val sum = (0 until limit).sumOf { (cpf[it] - '0') * (limit + 1 - it) }
                val rem = (sum * 10) % 11
                return if (rem >= 10) 0 else rem
            }
            return digit(9) == (cpf[9] - '0') && digit(10) == (cpf[10] - '0')
        }

        fun maskCpf(digits: String): String = buildString {
            digits.forEachIndexed { i, c ->
                if (i == 3 || i == 6) append('.')
                if (i == 9) append('-')
                append(c)
            }
        }

        private fun toTitleCase(s: String): String =
            s.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

        fun factory(repository: PosRepository) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CpfInputViewModel(repository) as T
            }
        }
    }
}
