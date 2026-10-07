package br.com.bliqbrasil.totem.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.com.bliqbrasil.totem.BliqTotemApp
import br.com.bliqbrasil.totem.data.model.WashOption
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import br.com.bliqbrasil.totem.ui.screens.activation.ActivationScreen
import br.com.bliqbrasil.totem.ui.screens.support.SupportScreen
import br.com.bliqbrasil.totem.ui.screens.activation.ActivationViewModel
import br.com.bliqbrasil.totem.ui.screens.checkout.CheckoutScreen
import br.com.bliqbrasil.totem.ui.screens.checkout.CheckoutViewModel
import br.com.bliqbrasil.totem.data.model.PoliticaCpf
import br.com.bliqbrasil.totem.ui.screens.cpfinput.CpfInputScreen
import br.com.bliqbrasil.totem.ui.screens.cpfinput.CpfInputViewModel
import br.com.bliqbrasil.totem.ui.screens.extras.ExtrasScreen
import br.com.bliqbrasil.totem.ui.screens.home.HomeScreen
import br.com.bliqbrasil.totem.ui.screens.home.HomeViewModel
import br.com.bliqbrasil.totem.ui.screens.minutespicker.MinutesPickerScreen
import br.com.bliqbrasil.totem.ui.screens.payment.PaymentScreen
import br.com.bliqbrasil.totem.ui.screens.payment.PaymentViewModel
import br.com.bliqbrasil.totem.ui.screens.session.SessionScreen
import br.com.bliqbrasil.totem.ui.screens.session.SessionViewModel
import br.com.bliqbrasil.totem.ui.screens.success.SuccessScreen
import br.com.bliqbrasil.totem.ui.screens.success.SuccessViewModel
import br.com.bliqbrasil.totem.ui.screens.welcome.WelcomeScreen
import br.com.bliqbrasil.totem.ui.screens.welcome.WelcomeViewModel

@Composable
fun NavGraph(navController: NavHostController, app: BliqTotemApp) {

    val startDestination: Any = if (app.tokenStorage.getToken() != null)
        AppDestinations.Welcome else AppDestinations.Activation

    NavHost(navController = navController, startDestination = startDestination) {

        composable<AppDestinations.Activation> {
            val vm = viewModel<ActivationViewModel>(
                factory = ActivationViewModel.factory(app.repository, app.tokenStorage)
            )
            ActivationScreen(
                viewModel = vm,
                onActivated = {
                    navController.navigate(AppDestinations.Welcome) {
                        popUpTo(AppDestinations.Activation) { inclusive = true }
                    }
                }
            )
        }

        composable<AppDestinations.Welcome> {
            val vm = viewModel<WelcomeViewModel>(
                factory = WelcomeViewModel.factory(app.repository, app.bleManager, app.tokenStorage)
            )
            WelcomeScreen(
                viewModel = vm,
                // Com a identificação desativada na franquia, o CPF sai do fluxo:
                // vai direto para a escolha do serviço, sem cliente vinculado.
                onStartFlow = {
                    val destino =
                        if (vm.state.value.politicaCpf == PoliticaCpf.DESATIVADO) AppDestinations.Home()
                        else AppDestinations.CpfInput
                    navController.navigate(destino)
                },
                onNeedActivation = {
                    navController.navigate(AppDestinations.Activation) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onResumeSession = { cicloId, totalMinutes, resumeFromSeconds, boxTipo ->
                    navController.navigate(
                        AppDestinations.Session(
                            cicloId           = cicloId,
                            totalMinutes      = totalMinutes,
                            boxTipo           = boxTipo,
                            resumeFromSeconds = resumeFromSeconds,
                        )
                    ) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        composable<AppDestinations.CpfInput> {
            val vm = viewModel<CpfInputViewModel>(
                factory = CpfInputViewModel.factory(app.repository, app.tokenStorage)
            )
            CpfInputScreen(
                viewModel = vm,
                onBack = {
                    // Voltar do CPF cancela a intenção e volta pro Welcome
                    navController.navigate(AppDestinations.Welcome) {
                        popUpTo(AppDestinations.Welcome) { inclusive = true }
                    }
                },
                onContinue = { clienteId ->
                    navController.navigate(AppDestinations.Home(clienteId = clienteId)) {
                        popUpTo(AppDestinations.CpfInput) { inclusive = true }
                    }
                }
            )
        }

        composable<AppDestinations.Home> { entry ->
            val route = entry.toRoute<AppDestinations.Home>()
            val vm = viewModel<HomeViewModel>(
                factory = HomeViewModel.factory(app, app.repository, app.bleManager, app.tokenStorage)
            )
            HomeScreen(
                viewModel = vm,
                clienteId = route.clienteId,
                onNavigateToExtras = { option, boxTipo, clienteId ->
                    navController.navigate(AppDestinations.Extras(option.toJson(), boxTipo, clienteId))
                },
                onNavigateToMinutesPicker = { option, boxTipo, clienteId ->
                    navController.navigate(AppDestinations.MinutesPicker(option.toJson(), boxTipo, clienteId))
                },
                onNavigateToCheckout = { option, extras, minutes, price, boxTipo, clienteId ->
                    navController.navigate(
                        AppDestinations.Checkout(
                            washOptionJson     = option.toJson(),
                            selectedExtrasJson = extras.toJson(),
                            totalMinutes       = minutes,
                            totalPrice         = price,
                            boxTipo            = boxTipo,
                            clienteId          = clienteId,
                        )
                    )
                },
                onNavigateToSession = { cicloId, totalMinutes, resumeFromSeconds, boxTipo ->
                    navController.navigate(
                        AppDestinations.Session(
                            cicloId            = cicloId,
                            totalMinutes       = totalMinutes,
                            boxTipo            = boxTipo,
                            resumeFromSeconds  = resumeFromSeconds,
                        )
                    ) { popUpTo(AppDestinations.Home::class) { inclusive = false } }
                },
                onNeedActivation = {
                    navController.navigate(AppDestinations.Activation) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSupport = {
                    navController.navigate(AppDestinations.Support)
                },
            )
        }

        composable<AppDestinations.MinutesPicker> { entry ->
            val route = entry.toRoute<AppDestinations.MinutesPicker>()
            val washOption = route.washOptionJson.toWashOption()
            MinutesPickerScreen(
                washOption = washOption,
                onBack = { navController.popBackStack() },
                onContinue = { minutes, totalPrice ->
                    navController.navigate(
                        AppDestinations.Checkout(
                            washOptionJson     = washOption.toJson(),
                            selectedExtrasJson = emptyList<WashOptionExtra>().toJson(),
                            totalMinutes       = minutes,
                            totalPrice         = totalPrice,
                            boxTipo            = route.boxTipo,
                            clienteId          = route.clienteId,
                        )
                    )
                }
            )
        }

        composable<AppDestinations.Extras> { entry ->
            val route = entry.toRoute<AppDestinations.Extras>()
            val washOption = route.washOptionJson.toWashOption()
            ExtrasScreen(
                washOption = washOption,
                onBack = { navController.popBackStack() },
                onContinue = { extras, minutes, price ->
                    navController.navigate(
                        AppDestinations.Checkout(
                            washOptionJson     = washOption.toJson(),
                            selectedExtrasJson = extras.toJson(),
                            totalMinutes       = minutes,
                            totalPrice         = price,
                            boxTipo            = route.boxTipo,
                            clienteId          = route.clienteId,
                        )
                    )
                }
            )
        }

        composable<AppDestinations.Checkout> { entry ->
            val route = entry.toRoute<AppDestinations.Checkout>()
            val washOption     = route.washOptionJson.toWashOption()
            val selectedExtras = route.selectedExtrasJson.toWashOptionExtras()
            val vm = viewModel<CheckoutViewModel>(
                factory = CheckoutViewModel.factory(
                    washOption     = washOption,
                    selectedExtras = selectedExtras,
                    totalMinutes   = route.totalMinutes,
                    totalPrice     = route.totalPrice,
                )
            )
            // Vindo da oferta do fim da sessão, o Checkout é a única tela na
            // pilha (a navegação limpou tudo): voltar leva à tela inicial.
            val goBack: () -> Unit = {
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    navController.navigate(AppDestinations.Welcome) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            BackHandler(onBack = goBack)
            CheckoutScreen(
                viewModel = vm,
                onBack = goBack,
                onConfirm = { method ->
                    navController.navigate(
                        AppDestinations.Payment(
                            washOptionJson     = washOption.toJson(),
                            selectedExtrasJson = selectedExtras.toJson(),
                            totalMinutes       = route.totalMinutes,
                            totalPrice         = route.totalPrice,
                            paymentMethod      = method.toNavString(),
                            boxTipo            = route.boxTipo,
                            clienteId          = route.clienteId,
                            crossSell          = route.crossSell,
                        )
                    )
                }
            )
        }

        composable<AppDestinations.Payment> { entry ->
            val route = entry.toRoute<AppDestinations.Payment>()
            val vm = viewModel<PaymentViewModel>(
                factory = PaymentViewModel.Factory(
                    stone          = app.stonePaymentManager,
                    repository     = app.repository,
                    washOption     = route.washOptionJson.toWashOption(),
                    selectedExtras = route.selectedExtrasJson.toWashOptionExtras(),
                    totalMinutes   = route.totalMinutes,
                    totalPrice     = route.totalPrice,
                    paymentMethod  = route.paymentMethod.toPaymentMethod(),
                    clienteId      = route.clienteId,
                    crossSell      = route.crossSell,
                )
            )
            PaymentScreen(
                viewModel = vm,
                onSuccess = { cicloId, acquirerKey ->
                    val washOption = route.washOptionJson.toWashOption()
                    navController.navigate(
                        AppDestinations.Success(
                            paymentMethod = route.paymentMethod,
                            totalMinutes  = route.totalMinutes,
                            totalPrice    = route.totalPrice,
                            cicloId       = cicloId,
                            transacaoId   = acquirerKey,
                            boxTipo       = route.boxTipo,
                            clienteId     = route.clienteId,
                            // Uma compra que já veio da oferta não oferece de novo:
                            // o cross-sell acontece uma vez por pacote.
                            crossSellMinutos = washOption.crossSellMinutos.takeIf { !route.crossSell },
                            crossSellPreco   = washOption.crossSellPreco.takeIf { !route.crossSell },
                        )
                    ) { popUpTo(AppDestinations.Checkout::class) { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppDestinations.Success> { entry ->
            val route = entry.toRoute<AppDestinations.Success>()
            val vm = viewModel<SuccessViewModel>(
                factory = SuccessViewModel.factory(
                    repository   = app.repository,
                    bleManager   = app.bleManager,
                    cicloId      = route.cicloId,
                    totalMinutes = route.totalMinutes,
                )
            )
            SuccessScreen(
                viewModel     = vm,
                paymentMethod = route.paymentMethod.toPaymentMethod(),
                totalMinutes  = route.totalMinutes,
                totalPrice    = route.totalPrice,
                onSessionReady = { cicloId ->
                    navController.navigate(
                        AppDestinations.Session(
                            cicloId          = cicloId,
                            totalMinutes     = route.totalMinutes,
                            boxTipo          = route.boxTipo,
                            paymentMethod    = route.paymentMethod,
                            totalPrice       = route.totalPrice,
                            clienteId        = route.clienteId,
                            crossSellMinutos = route.crossSellMinutos,
                            crossSellPreco   = route.crossSellPreco,
                        )
                    ) { popUpTo(AppDestinations.Success::class) { inclusive = true } }
                },
                onBackToHome = {
                    navController.navigate(AppDestinations.Welcome) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<AppDestinations.Session> { entry ->
            val route = entry.toRoute<AppDestinations.Session>()
            val vm = viewModel<SessionViewModel>(
                factory = SessionViewModel.factory(
                    repository        = app.repository,
                    bleManager        = app.bleManager,
                    cicloId           = route.cicloId,
                    totalMinutes      = route.totalMinutes,
                    resumeFromSeconds = route.resumeFromSeconds,
                    boxTipo           = route.boxTipo,
                    clienteId         = route.clienteId,
                    crossSellMinutos  = route.crossSellMinutos,
                    crossSellPreco    = route.crossSellPreco,
                    diagnostico       = app.diagnostico,
                )
            )
            SessionScreen(
                viewModel  = vm,
                onFinished = {
                    navController.navigate(AppDestinations.Welcome) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                // Aceitou os minutos extras: vira uma compra de minutagem avulsa,
                // pelo mesmo caminho de sempre (método de pagamento → Stone).
                onAcceptOffer = { offer ->
                    val avulso = WashOption(
                        id      = offer.produtoAvulsoId,
                        label   = "Mais ${offer.minutos} min",
                        minutes = offer.minutos,
                        price   = offer.preco,
                        tipo    = "MINUTAGEM_AVULSA",
                        extras  = emptyList(),
                    )
                    navController.navigate(
                        AppDestinations.Checkout(
                            washOptionJson     = avulso.toJson(),
                            selectedExtrasJson = emptyList<WashOptionExtra>().toJson(),
                            totalMinutes       = offer.minutos,
                            totalPrice         = offer.preco,
                            boxTipo            = route.boxTipo,
                            clienteId          = route.clienteId,
                            crossSell          = true,
                        )
                    ) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        composable<AppDestinations.Support> {
            SupportScreen(onBack = { navController.popBackStack() })
        }
    }
}
