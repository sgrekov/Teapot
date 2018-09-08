[![Maven Central](https://img.shields.io/maven-central/v/com.factorymarket.rxelm/rxelm.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.factorymarket.rxelm%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](https://github.com/FactoryMarketRetailGmbH/RxElm/blob/master/LICENSE)

# RxElm
Unidirectional Dataflow library for Android inspired by The Elm Architecture. 


## Dependency

```
implementation 'com.factorymarket.rxelm:rxelm:0.1.0'
//Testing utility
testImplementation 'com.factorymarket.rxelm:rxelm-test:0.1.0'
```


#### Snapshot
Use `0.2.0-SNAPSHOT` as your version number and add the url to the snapshot repository:

```
allprojects {
    repositories {     
        maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
    }
}

```

```
implementation 'com.factorymarket.rxelm:rxelm:0.2.0-SNAPSHOT'
```


## Concepts 
RxElm is heavily influenced by The Elm Architecture. Its core concepts:

Unidirectional dataflow
Immutable state
Managed side effects

It allows to write highly testable and predictable UI logic. RxElm is written in Kotlin and built 
on top of RxJava2. One of the main advantages of RxElm is simplicity and while RxElm relies on solid reactive foundation
of RxJava, it hides it's complexities and uses it only for managing side effects and external events.

### Core types
#### State 
This is the type for describing the state of your app or screen. 

#### Msg (short for Message)  
Base type for all events happening during interaction with UI (such as button click, text inputs, etc)

####Cmd (short for Command) 
Type for side-effects. If you create Cmd, that means you want to execute a particular side effect (http request or other IO operation).
When executed, the command will return new Msg with resulting data.

#### Function Update  
Function Update takes Msg and State as input, and returns a pair of two values — new State and Cmd, or simply speaking, what side effect you want to execute for incoming Msg. 
The main aspect of this function is that it is a pure function. That means there must be no side effects inside this function.

#### Function Render 
Takes State as an input, and renders view in declarative manner. 

## Getting Started

### Minimal implementation

```kotlin
class MyFragment : Fragment(), Component {

  
    private lateinit var plusBtn: Button
    private lateinit var minusBtn: Button
    private lateinit var counterText: TextView
    private lateinit var programDisposable: Disposable
    
    data class IncrementDecrementState(val value: Int = 0) : State()
    
    object Inc : Msg()
    object Dec : Msg()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
        val view = inflater.inflate(R.layout.main_layout, container, false)       

        val program = ProgramBuilder()
                        .outputScheduler(AndroidSchedulers.mainThread())
                        .build()
                        
        plusBtn = view.findViewById(R.id.plus_btn)
        minusBtn = view.findViewById(R.id.minus_btn)
        counterText = view.findViewById(R.id.counter_text)
        
        val counter = if (savedInstanceState != null) {
            savedInstanceState.getString("counter")           
        }
               
        programDisposable = program.init(IncrementDecrementState(value = counter ?: 0), this)                
    }
    
    override fun update(msg: Msg, state: counterText): Pair<State, Cmd> {          
            return when (msg) {
                is Init -> state to None
                is Inc -> state.copy(value = state.value + 1) to None               
                is Dec -> state.copy(value = state.value - 1) to None
                else -> state to None
            }
    }
    
    override fun render(state: IncrementDecrementState) {
        state.apply {
            counterText.showValue(value)
        }
    }
    
    override fun call(cmd: Cmd): Single<Msg> {
        return Single.just(Idle)         
    }
    
    override fun onSaveInstanceState(outState  : Bundle) {
      super.onSaveInstanceState(outState)
     
      outState.putString("counter", program.state().value)
    }
    
    @Override
    fun onDestroyView() {
      super.onDestroyView()
      programDisposable.dispose()
    }
    
}
```

### Wiki
To learn more, see the [wiki](https://github.com/FactoryMarketRetailGmbH/RxElm/wiki) for a user guide and best practices.

### Sample Project 
To see full working sample, see [sample App](https://github.com/FactoryMarketRetailGmbH/RxElm/tree/master/sample) 


### Resources
Taming state in Android with Elm Architecture and Kotlin [series of blog posts](https://proandroiddev.com/taming-state-in-android-with-elm-architecture-and-kotlin-part-1-566caae0f706)
[Official guide into The Elm Architecture](https://guide.elm-lang.org/architecture/)
