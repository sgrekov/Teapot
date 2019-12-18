package dev.teapot

import dev.teapot.msg.Idle
import dev.teapot.msg.Msg
import dev.teapot.program.ProgramBuilder
import dev.teapot.cmd.Cmd
import dev.teapot.contract.*
import dev.teapot.feature.CompositeFeature
import dev.teapot.feature.RxCompositeFeature
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CompositeComponentTest {

    data class Comp1State(val foo: String) : State()

    data class Comp1Msg(val foo: String) : Msg()
    object Comp1Msg2 : Msg()
    data class Comp1Cmd(val foo: String) : Cmd()

    class Feature1 : PluggableFeature<Comp1State>, RxEffectHandler {
        override fun handlesMessage(msg: Msg): Boolean = msg is Comp1Msg

        override fun handlesCommands(cmd: Cmd): Boolean = cmd is Comp1Cmd

        override fun initialState(): Comp1State = Comp1State(foo = "fooInitial")

        override fun update(msg: Msg, state: Comp1State): Update<Comp1State> = when (msg) {
            is Comp1Msg -> Update.state(state.copy(foo = msg.foo))
            else -> Update.idle()
        }

        override fun call(cmd: Cmd): Single<Msg> = when (cmd) {
            is Comp1Cmd -> Single.just(Comp1Msg2)
            else -> Single.just(Idle)
        }
    }

    val component1 = Feature1()

    data class MainState(val bar: String, val comp1State: Comp1State) : State()

    data class MainMsg1(val bar: String) : Msg()
    data class MainCmd1(val bar: String) : Cmd()

    class MainComponent(val feature1: Feature1) : PluggableFeature<MainState>, Renderable<MainState>, RxEffectHandler {

        override fun render(state: MainState) {}

        override fun handlesMessage(msg: Msg): Boolean = true

        override fun handlesCommands(cmd: Cmd): Boolean = true

        override fun initialState(): MainState = MainState(bar = "initialBar", comp1State = feature1.initialState())

        override fun update(msg: Msg, state: MainState): Update<MainState> = when (msg) {
            is MainMsg1 -> Update.state(state.copy(bar = msg.bar))
            else -> Update.idle()
        }

        override fun call(cmd: Cmd): Single<Msg> = when (cmd) {
            is MainCmd1 -> Single.just(Idle)
            else -> Single.just(Idle)
        }
    }

    lateinit var mainComp : MainComponent
    lateinit var compositeComponent : CompositeFeature<MainState>

    @Before
    fun setUp() {
        mainComp = MainComponent(component1)
        compositeComponent = RxCompositeFeature(ProgramBuilder().outputScheduler(Schedulers.trampoline()), mainComp)

        compositeComponent.addFeature(
                component1,
                { mainState -> mainState.comp1State },
                { subState, mainState -> mainState.copy(comp1State = subState) })

        compositeComponent.addMainFeature(mainComp)
    }

    @Test
    fun testStart() {
        compositeComponent.run(mainComp.initialState())
        Assert.assertEquals(compositeComponent.state()?.bar, "initialBar")
        Assert.assertEquals(compositeComponent.state()?.comp1State?.foo, "fooInitial")
    }

    @Test
    fun testSubComponentMsg() {
        compositeComponent.run(mainComp.initialState())
        compositeComponent.accept(Comp1Msg(foo = "foo1"))

        Assert.assertEquals("initialBar", compositeComponent.state()?.bar)
        Assert.assertEquals("foo1", compositeComponent.state()?.comp1State?.foo)
    }

    @Test
    fun testMainComponentMsg() {
        compositeComponent.run(mainComp.initialState())
        compositeComponent.accept(MainMsg1("bar1"))

        Assert.assertEquals("bar1", compositeComponent.state()?.bar)
        Assert.assertEquals("fooInitial", compositeComponent.state()?.comp1State?.foo)
    }

}