package ui

import gromiloff.observer.example.R
import observer.EmptyBaseModel
import observer.impl.ObserverActivity

class OneScreenActivity(override val layoutId: Int = R.layout.activity_master) : ObserverActivity<EmptyBaseModel>()