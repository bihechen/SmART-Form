//
//  ItemStore.swift
//  SmART-FORM
//
//  Created by Siyang Zhang on 6/5/17.
//  Copyright © 2017 Siyang Zhang. All rights reserved.
//

import UIKit

class TestStore {
    var tests = [Test]()
    @discardableResult func createTest() -> Test {
        let newTest = Test(id: "0", title: "newTest", result: "50 ppm", date: Date(), image: #imageLiteral(resourceName: "icon-osu-logo"), temperature: "70", humidity: "80")
        tests.append(newTest)
        
        return newTest
    }
}