#!/usr/bin/python

from generator import Sequence, Choice, Entry
from generator import generate
from generator import defaultActivityHook

import random

def notifyHook(t, attrs):
    # Urgente = ["Yes", "No"]
    # Urgente = [True, False]
    # Urgente = [0, 1]
    Urgente = [0.0, 1.0]
    urg = Urgente[random.randint(0, len(Urgente)-1)]
    attrs["urg"] = urg

    t0, t1 = defaultActivityHook(t, attrs)

    return t0,t1

def airHook(t, attrs):
    t0, t1 = defaultActitivyHook(t, attrs)
    if attrs["dest"] == "Italy":
        t1 += 0
    elif attrs["dest"] == "Spain" or attrs["dest"] == "UK":
        t1 += random.randint(1, 60 * 60)
    else:
        t1 += random.randint(60 * 60, 5 * 60 * 60)
    return t0,t1



def choiceHook(l, attrs):
    if  attrs["urg"] == "Yes":
        return l[1]
    return l[0]

process = Sequence([ Entry("NotifyBug", notifyHook),
                      Choice([
                          Sequence([ Entry("CheckBug"),
                                     Entry("FixBug" )
                               ]),
                          Entry("FixBug")
                          ], choiceHook)
                      ])
generate(process, 100)
