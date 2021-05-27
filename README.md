[![Bintray](https://img.shields.io/badge/Bintray-0.9.1-green.svg)](https://bintray.com/sgrekov/Teapot/Teapot/0.9.1)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](https://github.com/FactoryMarketRetailGmbH/RxElm/blob/master/LICENSE)

# Teapot
Unidirectional Dataflow library for Android inspired by The Elm Architecture. 


## Dependency


```
repositories {
    ...
    mavenCentral()
    ...
}

implementation 'dev.teapot:teapot:0.9.1'
testImplementation 'dev.teapot:teapottest:0.9.1'
```



## Concepts 
Teapot is heavily influenced by The Elm Architecture(TEA). Its core concepts:

* Unidirectional dataflow
* Immutable state
* Managed side effects

It allows to write highly testable and predictable UI logic. Teapot is written in Kotlin and supports executing side effects via RxJava2 or Coroutines.

### Core types
#### State 
This is the type for describing the state of your app or screen. 

#### Msg   
Base type for all events happening during interaction with UI (such as button click, text inputs, etc)

#### Cmd  
Type for side-effects. If you create Cmd, that means you want to execute a particular side effect (http request or other IO operation).
When executed, the command will return new Msg with resulting data.

#### Update()  
Function Update takes Msg and State as input, and returns a pair of two values — new State and Cmd, or simply speaking, what side effect you want to execute for incoming Msg. 
The main aspect of this function is that it is a pure function. That means there must be no side effects inside this function.

#### Render() 
Function render() takes State as an input, and renders view in declarative manner. 

## Getting Started

### Minimal implementation

```kotlin

data class IncrementDecrementState(val value: Int = 0) : State()
    
object Inc : Msg()
object Dec : Msg()    
    
class MyFragment : Fragment(), Upd<IncrementDecrementState>, Renderable<IncrementDecrementState> {

  
    private lateinit var plusBtn: Button
    private lateinit var minusBtn: Button
    private lateinit var counterText: TextView   
                    
    val program = ProgramBuilder()
                        .outputScheduler(AndroidSchedulers.mainThread())
                        .build(this)            
   

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
        
        val view = inflater.inflate(R.layout.main_layout, container, false)       
                        
        plusBtn = view.findViewById(R.id.plus_btn)
        minusBtn = view.findViewById(R.id.minus_btn)
        counterText = view.findViewById(R.id.counter_text)
               
        program.run(initialState = IncrementDecrementState(value = savedInstanceState?.getInt("counter", 0) ?: 0))              
    }
    
    override fun update(msg: Msg, state: counterText): Update<IncrementDecrementState> {          
            return when (msg) {            
                is Inc -> Update.state(state.copy(value = state.value + 1))               
                is Dec -> Update.state(state.copy(value = state.value - 1))
                else -> Update.idle()
            }
    }
    
    override fun render(state: IncrementDecrementState) {
        state.apply {
            counterText.setText(value)
        }
    }
    
    override fun onSaveInstanceState(outState  : Bundle) {
      super.onSaveInstanceState(outState)
     
      outState.putString("counter", program.state().value)
    }
    
    @Override
    fun onDestroyView() {
      super.onDestroyView()
      program.stop()
    }
    
}
```

### Sample Project 
To see full working sample, check out [the sample app](https://github.com/sgrekov/Teapot/tree/master/sample) 


### Resources
* Taming state in Android with Elm Architecture and Kotlin [series of blog posts](https://proandroiddev.com/taming-state-in-android-with-elm-architecture-and-kotlin-part-1-566caae0f706)
* [Official guide into The Elm Architecture](https://guide.elm-lang.org/architecture/)
